package com.music.scoring_system.controller;

import com.music.scoring_system.config.ScoreWebSocketHandler;
import com.music.scoring_system.entity.Staff;
import com.music.scoring_system.repository.StaffRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/api/staff")
@CrossOrigin
public class StaffController {

    @Autowired
    private StaffRepository staffRepository;

    @Autowired(required = false) // 防止未注入导致 500 崩溃
    private ScoreWebSocketHandler webSocketHandler;

    // 1. 获取全域所有【已审核通过】的幕后人员名单
    @GetMapping
    public List<Staff> getAllStaff() {
        return staffRepository.findByStatus("APPROVED");
    }

    // 🚀 新增：获取【待审核】的注册申请名单
    @GetMapping("/pending")
    public List<Staff> getPendingStaff() {
        return staffRepository.findByStatus("PENDING");
    }

    // 🚀 核心修复：终端自主注册接口 (加入了历史脏数据防崩与明文错误返回)
    @PostMapping("/register")
    public ResponseEntity<?> registerStaff(@RequestBody Staff staff) {
        try {
            // 1. 基础非空校验，阻止残缺数据引发的数据库崩溃
            if (staff.getName() == null || staff.getName().trim().isEmpty() ||
                    staff.getStudentNo() == null || staff.getStudentNo().trim().isEmpty()) {
                return ResponseEntity.badRequest().body("姓名或学号不能为空！");
            }

            // 2. 数据查重防崩 (加上对历史脏数据的容错)
            try {
                Optional<Staff> existing = staffRepository.findByNameAndStudentNo(staff.getName(), staff.getStudentNo());
                if (existing.isPresent()) {
                    return ResponseEntity.badRequest().body("该姓名和学号已被注册！");
                }
            } catch (Exception ex) {
                // 如果是由于历史测试脏数据抛出 NonUniqueResultException 崩溃，在此处拦截并直白反馈
                return ResponseEntity.badRequest().body("该学号已被多次重复注册，请联系总控后台清理脏数据档案！");
            }

            // 3. 补全缺失数据，防止触发 @Column(nullable = false) 报错
            staff.setStatus("PENDING"); // 默认待审核
            staff.setRequirePasswordChange(false);

            if (staff.getLevel() == null) {
                staff.setLevel(3); // 默认先赋予 3 级
            }
            if (staff.getRole() == null || staff.getRole().trim().isEmpty()) {
                staff.setRole("待分配"); // 极度关键：防止 role 为 null 引发 500
            }

            // 4. 赋予基础权限
            staff.setCanGlobalBroadcast(false);
            staff.setCanGroupBroadcast(true);
            staff.setCanSOS(true);
            staff.setCanDirectReport(false);
            staff.setManagePower(false);
            staff.setSilentMode(false);

            // 5. 存入数据库
            staffRepository.save(staff);

            // 🚀 6. 核心修复点：通过 WebSocket 强制唤醒导播后台，让其弹出审批卡片！
            if (webSocketHandler != null) {
                webSocketHandler.broadcastScore("{\"type\":\"TACTICS_UPDATE\"}");
            }

            // 成功则返回正常的提示文本
            return ResponseEntity.ok("注册已成功提交给导播后台，请耐心等待审批！");

        } catch (Exception e) {
            // 将真正的崩溃原因打印在 IDEA 控制台，同时将最底层的报错直接返回给手机屏幕！
            e.printStackTrace();
            return ResponseEntity.badRequest().body("服务器底层存储异常，请截图联系技术人员: " + e.toString());
        }
    }

    // 2. 后台强力录入新的幕后人员 (直接通过)
    @PostMapping
    public Staff addStaff(@RequestBody Staff staff) {
        staff.setStatus("APPROVED");
        if (staff.getPassword() == null || staff.getPassword().isEmpty()) {
            staff.setPassword("123456"); // 默认密码
            staff.setRequirePasswordChange(true); // 强迫首次登录改密
        }
        Staff savedStaff = staffRepository.save(staff);
        if (webSocketHandler != null) {
            webSocketHandler.broadcastScore("{\"type\":\"TACTICS_UPDATE\"}");
        }
        return savedStaff;
    }

    // 🚀 新增：导播审批通过注册申请
    @PostMapping("/{id}/approve")
    public ResponseEntity<?> approveStaff(@PathVariable Long id, @RequestParam Integer level) {
        Optional<Staff> opt = staffRepository.findById(id);
        if (opt.isPresent()) {
            Staff s = opt.get();
            s.setStatus("APPROVED");
            s.setLevel(level);
            staffRepository.save(s);
            if (webSocketHandler != null) {
                webSocketHandler.broadcastScore("{\"type\":\"TACTICS_UPDATE\"}");
            }
            return ResponseEntity.ok().build();
        }
        return ResponseEntity.notFound().build();
    }

    // 3. 吊销/驳回/删除人员 (物理删除)
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteStaff(@PathVariable Long id) {
        staffRepository.deleteById(id);
        if (webSocketHandler != null) {
            webSocketHandler.broadcastScore("{\"type\":\"TACTICS_UPDATE\"}");
        }
        return ResponseEntity.ok().build();
    }

    // 🚀 新增：一键重置密码
    @PostMapping("/{id}/reset-pwd")
    public ResponseEntity<?> resetPwd(@PathVariable Long id) {
        Optional<Staff> opt = staffRepository.findById(id);
        if (opt.isPresent()) {
            Staff s = opt.get();
            s.setPassword("123456");
            s.setRequirePasswordChange(true); // 标记必须改密
            staffRepository.save(s);
            return ResponseEntity.ok().build();
        }
        return ResponseEntity.notFound().build();
    }

    // 4. 更新人员信息
    @PutMapping("/{id}")
    public ResponseEntity<Staff> updateStaff(@PathVariable Long id, @RequestBody Staff staffDetails) {
        return staffRepository.findById(id)
                .map(staff -> {
                    staff.setName(staffDetails.getName());
                    staff.setStudentNo(staffDetails.getStudentNo());
                    staff.setRole(staffDetails.getRole());
                    staff.setLevel(staffDetails.getLevel());

                    staff.setCanGlobalBroadcast(staffDetails.getCanGlobalBroadcast());
                    staff.setCanGroupBroadcast(staffDetails.getCanGroupBroadcast());
                    staff.setCanSOS(staffDetails.getCanSOS());
                    staff.setCanDirectReport(staffDetails.getCanDirectReport());
                    staff.setManagePower(staffDetails.getManagePower());
                    staff.setSilentMode(staffDetails.getSilentMode());

                    Staff updatedStaff = staffRepository.save(staff);
                    if (webSocketHandler != null) {
                        webSocketHandler.broadcastScore("{\"type\":\"TACTICS_UPDATE\"}");
                    }
                    return ResponseEntity.ok(updatedStaff);
                })
                .orElse(ResponseEntity.notFound().build());
    }
}