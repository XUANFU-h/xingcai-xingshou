package com.music.scoring_system.controller;

import com.music.scoring_system.config.ScoreWebSocketHandler;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/tactics")
@CrossOrigin
public class TacticsConfigController {

    @Autowired
    private StringRedisTemplate redisTemplate;

    // 🚨 注入 WebSocket 通讯中枢
    @Autowired
    private ScoreWebSocketHandler webSocketHandler;

    // --- 1. 建制组别 (Groups) 的云端存取 ---
    @GetMapping("/groups")
    public ResponseEntity<String> getGroups() {
        String groups = redisTemplate.opsForValue().get("sys:groups");
        // 如果 Redis 没有数据，返回默认的三个组别
        if (groups == null) {
            groups = "[{\"id\":\"g1\",\"name\":\"舞台组\",\"isSystem\":true},{\"id\":\"g2\",\"name\":\"游走机位组\",\"isSystem\":false},{\"id\":\"g3\",\"name\":\"场务1组\",\"isSystem\":false}]";
        }
        return ResponseEntity.ok(groups);
    }

    @PostMapping("/groups")
    public ResponseEntity<String> saveGroups(@RequestBody String groupsJson) {
        redisTemplate.opsForValue().set("sys:groups", groupsJson);
        // 🚨 触发全域雷达：通知所有终端组别配置已变
        webSocketHandler.broadcastScore("{\"type\":\"TACTICS_UPDATE\"}");
        return ResponseEntity.ok("组别配置已云端保存");
    }

    // --- 2. 麦克风阵列 (Mics) 的云端存取 ---
    @GetMapping("/mics")
    public ResponseEntity<String> getMics() {
        String mics = redisTemplate.opsForValue().get("sys:mics");
        return ResponseEntity.ok(mics != null ? mics : "[]"); // 如果为空，前端有默认生成逻辑
    }

    @PostMapping("/mics")
    public ResponseEntity<String> saveMics(@RequestBody String micsJson) {
        redisTemplate.opsForValue().set("sys:mics", micsJson);
        // 🚨 触发全域雷达：通知舞台组终端麦克风配置已变
        webSocketHandler.broadcastScore("{\"type\":\"TACTICS_UPDATE\"}");
        return ResponseEntity.ok("麦克风配置已云端保存");
    }

    // --- 3. 主持人阵列 (Hosts) 的云端存取 ---
    @GetMapping("/hosts")
    public ResponseEntity<String> getHosts() {
        String hosts = redisTemplate.opsForValue().get("sys:hosts");
        if (hosts == null) {
            hosts = "[{\"id\":\"h1\",\"name\":\"男主持\"},{\"id\":\"h2\",\"name\":\"女主持\"}]";
        }
        return ResponseEntity.ok(hosts);
    }

    @PostMapping("/hosts")
    public ResponseEntity<String> saveHosts(@RequestBody String hostsJson) {
        redisTemplate.opsForValue().set("sys:hosts", hostsJson);
        // 🚨 触发全域雷达：通知所有终端主持人配置已变
        webSocketHandler.broadcastScore("{\"type\":\"TACTICS_UPDATE\"}");
        return ResponseEntity.ok("主持人配置已云端保存");
    }
}