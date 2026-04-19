package com.music.scoring_system.controller;

import com.music.scoring_system.entity.ChatMessage;
import com.music.scoring_system.entity.SystemLog;
import com.music.scoring_system.repository.ChatMessageRepository;
import com.music.scoring_system.repository.SystemLogRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.http.ResponseEntity;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/tactics")
public class HistoryController {

    @Autowired
    private ChatMessageRepository chatMessageRepository;

    @Autowired
    private SystemLogRepository systemLogRepository;

    @GetMapping("/history")
    public Map<String, Object> getHistory() {
        Map<String, Object> result = new HashMap<>();

        // 拉取最后 500 条并反转顺序
        List<ChatMessage> messages = chatMessageRepository.findTop500ByOrderByDbIdDesc();
        Collections.reverse(messages);

        List<SystemLog> logs = systemLogRepository.findTop500ByOrderByDbIdDesc();
        Collections.reverse(logs);

        result.put("messages", messages);
        result.put("logs", logs);

        return result;
    }

    // 🚀 新增：物理删除清空全部历史记录的接口
    @DeleteMapping("/history")
    public ResponseEntity<Void> clearHistory() {
        chatMessageRepository.deleteAll(); // 从数据库物理抹除所有聊天
        systemLogRepository.deleteAll();   // 从数据库物理抹除所有日志
        return ResponseEntity.ok().build();
    }
}