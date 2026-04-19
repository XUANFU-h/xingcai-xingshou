package com.music.scoring_system.config;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.music.scoring_system.entity.ChatMessage;
import com.music.scoring_system.entity.SystemLog;
import com.music.scoring_system.entity.Staff;
import com.music.scoring_system.repository.ChatMessageRepository;
import com.music.scoring_system.repository.SystemLogRepository;
import com.music.scoring_system.repository.StaffRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.io.IOException;
import java.time.LocalTime;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

@Component
public class ScoreWebSocketHandler extends TextWebSocketHandler {

    @Autowired
    private StringRedisTemplate redisTemplate;

    @Autowired
    private ChatMessageRepository chatMessageRepository;
    @Autowired
    private SystemLogRepository systemLogRepository;
    @Autowired
    private StaffRepository staffRepository;

    private final ObjectMapper objectMapper = new ObjectMapper();

    private final CopyOnWriteArrayList<WebSocketSession> sessions = new CopyOnWriteArrayList<>();
    private final Map<String, String> sessionUserMap = new ConcurrentHashMap<>();

    private static String cachedActiveSinger = "";
    private static String cachedNextSinger = "";
    private static int currentStageIndex = 0;
    private static String currentStageName = "⚪ 尚未开始";

    private static String cachedMicsPayload = "";
    private static final Map<String, JsonNode> activeTasksCache = new ConcurrentHashMap<>();

    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        sessions.add(session);
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
        String payload = message.getPayload();
        JsonNode node = objectMapper.readTree(payload);

        if (node.has("type")) {
            String type = node.get("type").asText();

            if ("PING".equals(type)) {
                if (node.has("username")) {
                    String username = node.get("username").asText();
                    sessionUserMap.put(session.getId(), username);
                    markUserOnlineStatus(username, true);
                }

                if (node.has("role") && "AUDIENCE".equals(node.get("role").asText()) && node.has("deviceId")) {
                    String deviceId = node.get("deviceId").asText();
                    redisTemplate.opsForZSet().add("sys:active_audience_zset", deviceId, System.currentTimeMillis());
                }

                session.sendMessage(new TextMessage("{\"type\":\"PONG\"}"));
                return;
            }

            if ("SYNC_BACKSTAGE".equals(type)) {
                String stateMsg = String.format("{\"type\":\"sys_state_change\",\"stageIndex\":%d,\"content\":\"%s\"}", currentStageIndex, currentStageName);
                session.sendMessage(new TextMessage(stateMsg));

                if (!cachedActiveSinger.isEmpty()) session.sendMessage(new TextMessage(cachedActiveSinger));
                if (!cachedNextSinger.isEmpty()) session.sendMessage(new TextMessage(cachedNextSinger));

                if (!cachedMicsPayload.isEmpty()) session.sendMessage(new TextMessage(cachedMicsPayload));

                if (!activeTasksCache.isEmpty()) {
                    for (JsonNode taskNode : activeTasksCache.values()) {
                        ObjectNode taskPayload = objectMapper.createObjectNode();
                        taskPayload.put("type", "NEW_TASK");
                        taskPayload.set("task", taskNode);
                        session.sendMessage(new TextMessage(taskPayload.toString()));
                    }
                }
                return;
            }

            if ("chat".equals(type) || "sos_chat".equals(type)) {
                saveChatMessageToDb(node, "sos_chat".equals(type));
                saveSystemLogFromChat(node, "sos_chat".equals(type));
            }

            if ("SOS_ACK".equals(type)) {
                handleSosAckPersistence(node);
            }

            if ("SHOW_SINGER".equals(type)) {
                cachedActiveSinger = payload;
            } else if ("NEXT_SINGER".equals(type) || "SHOW_NEXT_SINGER".equals(type) || "SET_NEXT_SINGER".equals(type)) {
                cachedNextSinger = payload;
            }

            if ("sys_state_change".equals(type)) {
                if (node.has("stageIndex")) currentStageIndex = node.get("stageIndex").asInt();
                if (node.has("content")) currentStageName = node.get("content").asText();
            }

            if ("SINGLE_ACTION".equals(type)) {
                String targetName = node.get("targetName").asText();
                String action = node.get("action").asText();

                Optional<Staff> targetStaff = staffRepository.findByName(targetName);
                if (targetStaff.isPresent() && targetStaff.get().getLevel() == 1) {
                    System.out.println("🛡️ 系统拦截：禁止对一级导演 [" + targetName + "] 执行 " + action + " 指令！");
                    return;
                }
            }

            if ("SYNC_MICS".equals(type)) {
                cachedMicsPayload = payload;
            }

            if ("NEW_TASK".equals(type)) {
                if (node.has("task")) {
                    JsonNode taskNode = node.get("task");
                    String taskId = taskNode.has("id") ? taskNode.get("id").asText() : "";
                    if (!taskId.isEmpty()) {
                        activeTasksCache.put(taskId, taskNode);
                    }
                }
            }

            if ("DELETE_TASK".equals(type)) {
                if (node.has("taskId")) {
                    String taskId = node.get("taskId").asText();
                    activeTasksCache.remove(taskId);

                    try {
                        SystemLog log = new SystemLog();
                        log.setId(System.currentTimeMillis());
                        log.setTime(LocalTime.now().toString().substring(0, 5));
                        log.setSender("总控枢纽");
                        log.setCategory("指挥");
                        log.setContent("强制撤销了战术任务指令 (ID: " + taskId + ")");
                        log.setLevel("danger");
                        systemLogRepository.save(log);
                    } catch (Exception e) {
                        System.err.println("撤销任务日志存储失败: " + e.getMessage());
                    }
                }
            }
        }

        broadcastScore(payload);
    }

    private void saveChatMessageToDb(JsonNode node, boolean isSos) {
        try {
            ChatMessage cm = new ChatMessage();
            cm.setId(System.currentTimeMillis());
            cm.setSender(node.has("sender") ? node.get("sender").asText() : "未知");
            cm.setRole(node.has("role") ? node.get("role").asText() : "未知组别");
            cm.setLevel(node.has("level") ? node.get("level").asInt() : 3);
            cm.setChannel(node.has("channel") ? node.get("channel").asText() : "group");
            cm.setTargetGroup(node.has("targetGroup") && !node.get("targetGroup").isNull() ? node.get("targetGroup").asText() : null);
            cm.setContent(node.has("content") ? node.get("content").asText() : "");
            cm.setIsSos(isSos);
            cm.setTime(LocalTime.now().toString().substring(0, 5));
            chatMessageRepository.save(cm);
        } catch (Exception e) {
            System.err.println("MySQL 消息存储失败: " + e.getMessage());
        }
    }

    private void saveSystemLogFromChat(JsonNode node, boolean isSos) {
        try {
            SystemLog log = new SystemLog();
            log.setId(System.currentTimeMillis());
            log.setTime(LocalTime.now().toString().substring(0, 5));
            String sender = node.has("sender") ? node.get("sender").asText() : "未知";
            log.setSender(sender);
            log.setCategory("导演组".equals(sender) ? "指挥" : "行为");
            log.setContent(node.has("content") ? node.get("content").asText() : "");
            log.setLevel(isSos ? "danger" : "info");
            systemLogRepository.save(log);
        } catch (Exception e) {
            System.err.println("MySQL 日志存储失败: " + e.getMessage());
        }
    }

    private void handleSosAckPersistence(JsonNode node) {
        String content = node.has("content") ? node.get("content").asText() : "";
        String sender = node.has("sender") ? node.get("sender").asText() : "";

        ChatMessage sosMsg = chatMessageRepository.findFirstByContentAndIsSosTrueOrderByDbIdDesc(content);
        if (sosMsg != null) {
            String currentAcks = sosMsg.getAcks();
            if (currentAcks == null) currentAcks = "";
            if (!currentAcks.contains(sender)) {
                sosMsg.setAcks(currentAcks.isEmpty() ? sender : currentAcks + "," + sender);
                chatMessageRepository.save(sosMsg);
            }
        }
    }

    private void markUserOnlineStatus(String username, boolean online) {
        staffRepository.findByName(username).ifPresent(staff -> {
            // 这里可以更新 Staff 实体中的在线字段或最后活跃时间字段
        });
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) throws Exception {
        sessions.remove(session);
        String username = sessionUserMap.remove(session.getId());
        if (username != null) {
            redisTemplate.opsForSet().remove("sys:onlineCadres", username);
            broadcastScore("{\"type\": \"CADRE_STATUS_UPDATE\"}");
        }
    }

    public void broadcastScore(String message) {
        for (WebSocketSession session : sessions) {
            if (session.isOpen()) {
                try {
                    session.sendMessage(new TextMessage(message));
                } catch (IOException e) {
                    System.err.println("WS 广播失败: " + e.getMessage());
                }
            }
        }
    }
}