package com.music.scoring_system.controller;

import com.music.scoring_system.config.ScoreWebSocketHandler;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/command")
@CrossOrigin
public class CommandController {

    @Autowired
    private ScoreWebSocketHandler webSocketHandler;

    // 接收管理员的指令，并原封不动地广播给大屏幕
    @PostMapping("/send")
    public ResponseEntity<String> sendCommand(@RequestBody String commandJson) {
        webSocketHandler.broadcastScore(commandJson);
        return ResponseEntity.ok("指令已发送至大屏");
    }
}