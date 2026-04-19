package com.music.scoring_system.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.music.scoring_system.config.ScoreWebSocketHandler;
import com.music.scoring_system.dto.ScoreSubmitDTO;
import com.music.scoring_system.entity.Contestant;
import com.music.scoring_system.entity.ScoreRecord;
import com.music.scoring_system.entity.Staff;
import com.music.scoring_system.repository.*;
import com.music.scoring_system.util.JwtUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/scores")
@CrossOrigin
public class ScoreController {

    @Autowired private ScoreWebSocketHandler webSocketHandler;
    @Autowired private ScoreRecordRepository scoreRepo;
    @Autowired private ContestantRepository contestantRepo;
    @Autowired private JudgeRepository judgeRepo;
    @Autowired private StaffRepository staffRepository;
    @Autowired private StringRedisTemplate redisTemplate;
    @Autowired private JwtUtils jwtUtils;

    // --- Redis 状态读取封装 ---
    private String getSysString(String key, String defaultVal) { String val = redisTemplate.opsForValue().get("sys:" + key); return val != null ? val : defaultVal; }
    private void setSysString(String key, String value) { if (value == null) redisTemplate.delete("sys:" + key); else redisTemplate.opsForValue().set("sys:" + key, value); }
    private boolean getSysBoolean(String key, boolean defaultVal) { String val = getSysString(key, null); return val != null ? Boolean.parseBoolean(val) : defaultVal; }
    private double getSysDouble(String key, double defaultVal) { String val = getSysString(key, null); return val != null ? Double.parseDouble(val) : defaultVal; }
    private int getSysInt(String key, int defaultVal) { String val = getSysString(key, null); return val != null ? Integer.parseInt(val) : defaultVal; }

    public String getCurrentSession() { return getSysString("currentSession", "默认赛段"); }
    private String getAdminPwd() { return getSysString("adminPassword", "888"); }
    private boolean isVotingOpen() { return getSysBoolean("isVotingOpen", false); }
    private double getJudgeWeight() { return getSysDouble("judgeWeightRatio", 0.6); }

    @PostMapping("/generate-ticket")
    public ResponseEntity<Map<String, String>> generateTicket() {
        String newTicket = java.util.UUID.randomUUID().toString().replace("-", "").substring(0, 8);
        redisTemplate.opsForValue().set("sys:audience_ticket", newTicket);
        Map<String, String> response = new HashMap<>();
        response.put("ticket", newTicket);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/current-ticket")
    public ResponseEntity<Map<String, String>> getCurrentTicket() {
        String currentTicket = redisTemplate.opsForValue().get("sys:audience_ticket");
        if (currentTicket == null) currentTicket = "NOT_SET";
        Map<String, String> response = new HashMap<>();
        response.put("ticket", currentTicket);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/config")
    public ResponseEntity<?> getSystemConfig(@RequestParam(required = false) String deviceId, @RequestParam(required = false) String isCadre) {
        long now = System.currentTimeMillis();
        if (deviceId != null) {
            if ("true".equals(isCadre)) {
                redisTemplate.opsForSet().add("sys:onlineCadres", deviceId);
            } else {
                redisTemplate.opsForZSet().add("sys:active_audience_zset", deviceId, now);
            }
        }

        int maxAudience = getSysInt("maxAudienceCount", 500);
        redisTemplate.opsForZSet().removeRangeByScore("sys:active_audience_zset", 0, now - 15000);
        Long currentAudience = redisTemplate.opsForZSet().zCard("sys:active_audience_zset");

        String activeContestantStr = getSysString("activeContestantId", null);

        Map<String, Object> config = new HashMap<>();
        config.put("judgeWeight", getJudgeWeight());
        config.put("scoreScale", getSysInt("scoreScale", 100));
        config.put("currentSession", getCurrentSession());
        config.put("maxAudience", maxAudience);
        config.put("currentAudience", currentAudience != null ? currentAudience : 0L);
        config.put("isVotingOpen", isVotingOpen());
        config.put("lockAudience", getSysBoolean("lockAudience", false));
        config.put("lockTeacher", getSysBoolean("lockTeacher", false));
        config.put("lockCadre", getSysBoolean("lockCadre", false));
        config.put("isFull", (currentAudience != null ? currentAudience : 0L) >= maxAudience);
        config.put("activeContestantId", activeContestantStr != null ? Integer.parseInt(activeContestantStr) : null);

        config.put("trimRate", getSysInt("trimRate", 10));

        return ResponseEntity.ok(config);
    }

    @GetMapping("/online-cadres")
    public ResponseEntity<?> getOnlineCadres() { return ResponseEntity.ok(redisTemplate.opsForSet().members("sys:onlineCadres")); }

    @PostMapping("/kick-cadre")
    public ResponseEntity<String> kickCadre(@RequestParam String deviceId) {
        Long removed = redisTemplate.opsForSet().remove("sys:onlineCadres", deviceId);
        if(removed != null && removed > 0) {
            webSocketHandler.broadcastScore(String.format("{\"type\": \"KICK_CADRE\", \"deviceId\": \"%s\"}", deviceId));
            return ResponseEntity.ok("该设备已被强制登出");
        }
        return ResponseEntity.badRequest().body("设备未在线或未授权");
    }

    @PostMapping("/auth")
    public ResponseEntity<?> verifyAuth(@RequestParam String role,
                                        @RequestParam(required = false) String pwd,
                                        @RequestParam(required = false) String name,
                                        @RequestParam(required = false) String studentNo,
                                        @RequestParam(required = false) String ticket) {
        if ("admin".equals(role)) {
            if (getAdminPwd().equals(pwd)) {
                String token = jwtUtils.generateToken("admin");
                Map<String, String> res = new HashMap<>();
                res.put("token", token);
                res.put("msg", "OK");
                return ResponseEntity.ok(res);
            }
            return ResponseEntity.status(412).body("导播口令错误，拒绝访问");
        }

        if ("staff".equals(role)) {
            Optional<Staff> opt = staffRepository.findByNameAndStudentNo(name, studentNo);
            if (!opt.isPresent()) {
                return ResponseEntity.status(401).body("{\"error\": \"查无此人，请先注册申请接入\"}");
            }
            Staff staff = opt.get();
            if ("PENDING".equals(staff.getStatus())) {
                return ResponseEntity.status(403).body("{\"error\": \"账号正在等待导播总台审核，请稍后...\"}");
            }
            if (staff.getPassword() == null || !staff.getPassword().equals(pwd)) {
                return ResponseEntity.status(401).body("{\"error\": \"终端验证密码错误！\"}");
            }
            String token = jwtUtils.generateToken("staff");
            Map<String, Object> resp = new HashMap<>();
            resp.put("token", token);
            resp.put("msg", "OK");
            resp.put("requirePasswordChange", staff.getRequirePasswordChange());
            return ResponseEntity.ok(resp);
        }

        if ("judge".equals(role)) {
            Map<String, String> res = new HashMap<>();
            res.put("msg", "OK");
            return ResponseEntity.ok(res);
        }

        if ("audience".equals(role)) {
            String currentTicket = redisTemplate.opsForValue().get("sys:audience_ticket");
            if (currentTicket == null || "NOT_SET".equals(currentTicket) || !currentTicket.equals(ticket)) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                        .body("{\"error\": \"入场通行证无效或已过期，请扫描现场最新二维码！\"}");
            }
            String token = jwtUtils.generateToken("audience");
            Map<String, String> res = new HashMap<>();
            res.put("token", token);
            res.put("msg", "OK");
            return ResponseEntity.ok(res);
        }
        return ResponseEntity.status(412).body("未知角色");
    }

    @PostMapping("/update-staff-pwd")
    public ResponseEntity<?> updateStaffPwd(@RequestParam String name,
                                            @RequestParam String studentNo,
                                            @RequestParam String oldPwd,
                                            @RequestParam String newPwd) {
        Optional<Staff> opt = staffRepository.findByNameAndStudentNo(name, studentNo);
        if (opt.isPresent()) {
            Staff s = opt.get();
            if (!s.getPassword().equals(oldPwd)) {
                return ResponseEntity.badRequest().body("{\"error\": \"原终端验证密码错误\"}");
            }
            s.setPassword(newPwd);
            s.setRequirePasswordChange(false);
            staffRepository.save(s);
            return ResponseEntity.ok("{\"message\": \"密码已安全重置\"}");
        }
        return ResponseEntity.badRequest().body("{\"error\": \"系统错误\"}");
    }

    @PostMapping("/update-pwd")
    public ResponseEntity<String> updatePwd(@RequestParam String pwd) {
        setSysString("adminPassword", pwd);
        return ResponseEntity.ok("总台密码更新成功！");
    }

    @PostMapping("/set-limit")
    public ResponseEntity<String> setAudienceLimit(@RequestParam Integer limit) {
        setSysString("maxAudienceCount", String.valueOf(limit)); return ResponseEntity.ok("大众上限已调整");
    }

    @DeleteMapping("/clear-audience")
    public ResponseEntity<String> clearAudienceData() {
        redisTemplate.delete("sys:active_audience_zset");
        redisTemplate.delete("sys:activeDevices");
        return ResponseEntity.ok("大众端连接数据已彻底清空");
    }

    @PostMapping("/set-active")
    public ResponseEntity<?> setActiveContestant(@RequestParam Integer id) {
        setSysString("activeContestantId", String.valueOf(id)); return ResponseEntity.ok("OK");
    }

    @PostMapping("/toggle-voting")
    public ResponseEntity<String> toggleVoting(@RequestParam Boolean open) {
        setSysString("isVotingOpen", String.valueOf(open));
        webSocketHandler.broadcastScore(String.format("{\"type\": \"%s\", \"isOpen\": %b}", open ? "FORCE_VOTE_START" : "FORCE_VOTE_STOP", open));
        return ResponseEntity.ok(open ? "投票通道已开启" : "投票通道已关闭");
    }

    @PostMapping("/emergency-lock")
    public ResponseEntity<String> emergencyLock(@RequestParam String target, @RequestParam Boolean isLocked) {
        if ("AUDIENCE".equals(target)) setSysString("lockAudience", String.valueOf(isLocked));
        else if ("TEACHER".equals(target)) setSysString("lockTeacher", String.valueOf(isLocked));
        else if ("CADRE".equals(target)) setSysString("lockCadre", String.valueOf(isLocked));
        webSocketHandler.broadcastScore(String.format("{\"type\": \"EMERGENCY_LOCK\", \"target\": \"%s\", \"isLocked\": %b}", target, isLocked));
        return ResponseEntity.ok(isLocked ? "通道已被强行熔断！" : "熔断已解除。");
    }

    @PostMapping("/set-trim-rate")
    public ResponseEntity<String> setTrimRate(@RequestParam Integer rate) {
        setSysString("trimRate", String.valueOf(rate));
        return ResponseEntity.ok("防作弊滤除比率已应用");
    }

    @PostMapping("/submit")
    @Transactional
    public ResponseEntity<String> submitScore(@RequestBody ScoreSubmitDTO dto) {
        if (!isVotingOpen()) return ResponseEntity.badRequest().body("导播未开启打分环节");
        int scoreScale = getSysInt("scoreScale", 100);
        if (dto.getScore() > scoreScale || dto.getScore() < 0) return ResponseEntity.badRequest().body("分数非法");

        if ("STUDENT".equals(dto.getJudgeType())) {
            if (getSysBoolean("lockAudience", false)) return ResponseEntity.status(403).body("🚨 通道已被强行锁死！");
            if (dto.getDeviceId() != null && scoreRepo.existsByContestantIdAndDeviceId(dto.getContestantId(), dto.getDeviceId())) {
                return ResponseEntity.status(403).body("同一设备不可重复打分");
            }
        } else if ("JUDGE".equals(dto.getJudgeType())) {
            if (dto.getJudgeName().startsWith("CADRE_") && getSysBoolean("lockCadre", false)) return ResponseEntity.status(403).body("🚨 干部通道已被锁死！");
            else if (dto.getJudgeName().startsWith("TEACHER_") && getSysBoolean("lockTeacher", false)) return ResponseEntity.status(403).body("🚨 教师通道已被锁死！");
        }

        Contestant targetContestant = contestantRepo.findById(dto.getContestantId()).orElse(null);
        if (targetContestant == null) return ResponseEntity.badRequest().body("找不到选手");

        // 保存当前选手的真实得分记录
        ScoreRecord record = new ScoreRecord();
        record.setContestantId(dto.getContestantId());
        record.setJudgeName(dto.getJudgeName());
        record.setScoreValue(dto.getScore());
        record.setJudgeType(dto.getJudgeType());
        record.setSessionName(getCurrentSession());
        record.setDeviceId(dto.getDeviceId());
        scoreRepo.save(record);
        updateContestantScores(dto.getContestantId());

        webSocketHandler.broadcastScore(String.format("{\"type\": \"JUDGE_SCORE\", \"judgeName\": \"%s\", \"judgeScore\": %.2f, \"judgeType\": \"%s\", \"contestantId\": %d}", dto.getJudgeName(), dto.getScore(), dto.getJudgeType(), dto.getContestantId()));

        return ResponseEntity.ok("分数提交成功！");
    }

    private void updateContestantScores(Integer contestantId) {
        List<ScoreRecord> allScores = scoreRepo.findByContestantId(contestantId);
        double judgeAvg = calculateSmartAvg(allScores.stream().filter(s -> "JUDGE".equals(s.getJudgeType())).collect(Collectors.toList()));
        double studentAvg = calculateSmartAvg(allScores.stream().filter(s -> "STUDENT".equals(s.getJudgeType())).collect(Collectors.toList()));
        double weight = getJudgeWeight();
        Contestant c = contestantRepo.findById(contestantId).orElse(null);
        if (c != null) {
            c.setJudgeAvgScore(Math.round(judgeAvg * 100.0) / 100.0); c.setStudentAvgScore(Math.round(studentAvg * 100.0) / 100.0);
            c.setFinalScore(Math.round(((judgeAvg * weight) + (studentAvg * (1.0 - weight))) * 100.0) / 100.0); contestantRepo.save(c);
        }
    }

    private double calculateSmartAvg(List<ScoreRecord> scores) {
        if (scores.isEmpty()) return 0.0;
        List<Double> sorted = scores.stream().map(ScoreRecord::getScoreValue).sorted().collect(Collectors.toList());

        int trimRate = getSysInt("trimRate", 10);
        if (sorted.size() < 5 || trimRate <= 0) {
            return sorted.stream().mapToDouble(d -> d).average().orElse(0.0);
        }

        int trimCount = (int) Math.round(sorted.size() * (trimRate / 100.0));
        if (trimCount * 2 >= sorted.size()) {
            trimCount = Math.max(1, (sorted.size() - 1) / 2);
        }

        return sorted.subList(trimCount, sorted.size() - trimCount).stream().mapToDouble(d -> d).average().orElse(0.0);
    }

    private void backupAndClean(String operation) {
        CompletableFuture.runAsync(() -> {
            try {
                String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
                String backupKey = "sys:snapshot:" + operation + "_" + timestamp;

                Map<String, Object> dump = new HashMap<>();
                dump.put("contestants", contestantRepo.findAll());
                dump.put("scores", scoreRepo.findAll());

                ObjectMapper mapper = new ObjectMapper();
                String json = mapper.writeValueAsString(dump);

                redisTemplate.opsForValue().set(backupKey, json, 7, TimeUnit.DAYS);

                String listKey = "sys:snapshot_list";
                redisTemplate.opsForList().leftPush(listKey, backupKey);

                Long size = redisTemplate.opsForList().size(listKey);
                if (size != null && size > 15) {
                    String oldest = redisTemplate.opsForList().rightPop(listKey);
                    if (oldest != null) redisTemplate.delete(oldest);
                }
                System.out.println("✅ 异步云端快照生成成功: " + backupKey);
            } catch (Exception e) {
                System.err.println("❌ 快照生成失败: " + e.getMessage());
            }
        });
    }

    @GetMapping("/snapshots")
    public ResponseEntity<?> getSnapshots() {
        List<String> keys = redisTemplate.opsForList().range("sys:snapshot_list", 0, -1);
        if (keys == null) keys = new ArrayList<>();
        return ResponseEntity.ok(keys);
    }

    @PostMapping("/restore-snapshot")
    @Transactional
    public ResponseEntity<String> restoreSnapshot(@RequestParam String snapshotKey) {
        String json = redisTemplate.opsForValue().get(snapshotKey);
        if (json == null) return ResponseEntity.badRequest().body("快照已过期或不存在");
        try {
            ObjectMapper mapper = new ObjectMapper();
            JsonNode root = mapper.readTree(json);

            scoreRepo.deleteAll();
            contestantRepo.deleteAll();

            List<Contestant> contestants = Arrays.asList(mapper.treeToValue(root.get("contestants"), Contestant[].class));
            contestantRepo.saveAll(contestants);

            List<ScoreRecord> scores = Arrays.asList(mapper.treeToValue(root.get("scores"), ScoreRecord[].class));
            scoreRepo.saveAll(scores);

            return ResponseEntity.ok("时空回溯成功！所有数据已恢复至快照记录状态。");
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(500).body("快照恢复失败：" + e.getMessage());
        }
    }

    @PostMapping("/set-scale")
    public ResponseEntity<String> setScoreScale(@RequestParam Integer scale) {
        setSysString("scoreScale", String.valueOf(scale));
        webSocketHandler.broadcastScore(String.format("{\"type\": \"SCALE_CHANGED\", \"scale\": %d}", scale)); return ResponseEntity.ok("已切换");
    }

    @PostMapping("/set-weight")
    public ResponseEntity<String> setWeight(@RequestParam Double judgeWeight) {
        setSysString("judgeWeightRatio", String.valueOf(judgeWeight));
        List<Contestant> currentSessionContestants = contestantRepo.findBySessionName(getCurrentSession());
        for (Contestant c : currentSessionContestants) {
            double jAvg = c.getJudgeAvgScore() != null ? c.getJudgeAvgScore() : 0.0; double sAvg = c.getStudentAvgScore() != null ? c.getStudentAvgScore() : 0.0;
            c.setFinalScore(Math.round(((jAvg * judgeWeight) + (sAvg * (1.0 - judgeWeight))) * 100.0) / 100.0);
        }
        contestantRepo.saveAll(currentSessionContestants);
        return ResponseEntity.ok("权重算法重写，总成绩已更新");
    }

    @GetMapping("/details/{contestantId}")
    public List<ScoreRecord> getScoreDetails(@PathVariable Integer contestantId) { return scoreRepo.findByContestantId(contestantId); }

    @GetMapping("/judge-scores/{judgeId}")
    public ResponseEntity<?> getJudgeScores(@PathVariable Integer judgeId) {
        com.music.scoring_system.entity.Judge judge = judgeRepo.findById(judgeId).orElse(null);
        if (judge == null) return ResponseEntity.badRequest().body("找不到评委");
        String jName = judge.getName(); String currentSession = getCurrentSession();
        List<Map<String, Object>> result = new ArrayList<>();
        scoreRepo.findAll().stream().filter(s -> currentSession.equals(s.getSessionName()) && jName.equals(s.getJudgeName())).forEach(sr -> {
            Map<String, Object> map = new HashMap<>(); map.put("contestantId", sr.getContestantId()); map.put("scoreValue", sr.getScoreValue()); result.add(map);
        });
        return ResponseEntity.ok(result);
    }

    @PostMapping("/manual-update-aud/{id}")
    public ResponseEntity<String> manualUpdateAudScore(@PathVariable Integer id, @RequestParam Double score) {
        Contestant c = contestantRepo.findById(id).orElse(null);
        if (c != null) {
            c.setStudentAvgScore(score);
            double jAvg = c.getJudgeAvgScore() != null ? c.getJudgeAvgScore() : 0.0; double weight = getJudgeWeight();
            c.setFinalScore(Math.round(((jAvg * weight) + (score * (1.0 - weight))) * 100.0) / 100.0); contestantRepo.save(c);
            return ResponseEntity.ok("该选手大众均分已强制修改为：" + score);
        }
        return ResponseEntity.badRequest().body("找不到选手");
    }

    @PostMapping("/manual-update/{id}")
    public ResponseEntity<String> manualUpdateScore(@PathVariable Integer id, @RequestParam Double score) {
        backupAndClean("manual_update");
        Contestant c = contestantRepo.findById(id).orElse(null);
        if (c != null) { c.setFinalScore(score); contestantRepo.save(c); return ResponseEntity.ok("总分强制修改成功！"); }
        return ResponseEntity.badRequest().body("找不到选手");
    }

    @PutMapping("/update-info/{id}")
    public ResponseEntity<String> updateContestantInfo(@PathVariable Integer id, @RequestBody Contestant updatedInfo) {
        Contestant c = contestantRepo.findById(id).orElse(null);
        if (c != null) {
            c.setName(updatedInfo.getName());
            c.setClassName(updatedInfo.getClassName());
            c.setSongName(updatedInfo.getSongName());
            c.setPhotoUrl(updatedInfo.getPhotoUrl());
            contestantRepo.save(c);
            return ResponseEntity.ok("信息更新成功！");
        }
        return ResponseEntity.badRequest().body("找不到选手");
    }

    @Transactional
    @DeleteMapping("/clear-all")
    public ResponseEntity<String> clearAllScores() {
        backupAndClean("clear_all"); String currentSession = getCurrentSession(); scoreRepo.deleteBySessionName(currentSession);
        List<Contestant> currentSessionContestants = contestantRepo.findBySessionName(currentSession);
        for (Contestant c : currentSessionContestants) { c.setFinalScore(0.0); c.setJudgeAvgScore(0.0); c.setStudentAvgScore(0.0); }
        contestantRepo.saveAll(currentSessionContestants);
        return ResponseEntity.ok("数据已清空，且已自动生成快照。如有误操作，请联系场务");
    }

    @GetMapping("/session-history")
    public ResponseEntity<?> getSessionHistory() {
        String currentSession = getCurrentSession();
        List<Map<String, Object>> history = contestantRepo.findAll().stream().collect(Collectors.groupingBy(c -> c.getSessionName() == null ? "默认赛段" : c.getSessionName(), Collectors.counting())).entrySet().stream().map(e -> { Map<String, Object> map = new HashMap<>(); map.put("name", e.getKey()); map.put("contestantCount", e.getValue()); return map; }).collect(Collectors.toList());
        if (history.stream().noneMatch(m -> currentSession.equals(m.get("name")))) { Map<String, Object> currentMap = new HashMap<>(); currentMap.put("name", currentSession); currentMap.put("contestantCount", 0); history.add(currentMap); }
        return ResponseEntity.ok(history);
    }

    @PostMapping("/switch-session")
    public ResponseEntity<String> switchSession(@RequestParam String sessionName) {
        if (sessionName == null || sessionName.trim().isEmpty()) return ResponseEntity.badRequest().body("赛段名不能为空");
        setSysString("currentSession", sessionName); setSysString("activeContestantId", null); setSysString("isVotingOpen", "false");
        redisTemplate.delete("sys:active_audience_zset");
        redisTemplate.delete("sys:activeDevices");
        webSocketHandler.broadcastScore(String.format("{\"type\": \"FORCE_VOTE_STOP\", \"isOpen\": %b}", false));
        return ResponseEntity.ok("已成功切换至赛段：" + sessionName);
    }

    @Transactional
    @DeleteMapping("/delete-session")
    public ResponseEntity<?> deleteSession(@RequestParam String sessionName) {
        if (sessionName.equals(getCurrentSession())) return ResponseEntity.badRequest().body("不可删除当前运行赛段！");
        backupAndClean("delete_session_" + sessionName);
        scoreRepo.deleteAll(scoreRepo.findAll().stream().filter(s -> sessionName.equals(s.getSessionName())).collect(Collectors.toList()));
        List<Contestant> contestantsToDelete = contestantRepo.findBySessionName(sessionName);
        if (contestantsToDelete != null && !contestantsToDelete.isEmpty()) contestantRepo.deleteAll(contestantsToDelete);
        return ResponseEntity.ok("赛段数据已被永久抹除");
    }
}