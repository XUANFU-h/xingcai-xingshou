package com.music.scoring_system.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "chat_messages")
public class ChatMessage {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long dbId;

    private Long id; // 前端生成的时间戳ID
    private String sender;
    private String role;

    @Column(name = "user_level")
    private Integer level;

    private String channel;
    private String targetGroup;
    private String content;
    private String time;
    private Boolean isSos;

    @Column(columnDefinition = "TEXT")
    private String acks; // 用逗号分隔的已确认名单，或者存 JSON

    private LocalDateTime createTime;

    @PrePersist
    protected void onCreate() {
        createTime = LocalDateTime.now();
    }

    // --- 以下是 Getters 和 Setters (如果你用了 @Data 注解可以删掉这些) ---
    public Long getDbId() { return dbId; }
    public void setDbId(Long dbId) { this.dbId = dbId; }
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getSender() { return sender; }
    public void setSender(String sender) { this.sender = sender; }
    public String getRole() { return role; }
    public void setRole(String role) { this.role = role; }
    public Integer getLevel() { return level; }
    public void setLevel(Integer level) { this.level = level; }
    public String getChannel() { return channel; }
    public void setChannel(String channel) { this.channel = channel; }
    public String getTargetGroup() { return targetGroup; }
    public void setTargetGroup(String targetGroup) { this.targetGroup = targetGroup; }
    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }
    public String getTime() { return time; }
    public void setTime(String time) { this.time = time; }
    public Boolean getIsSos() { return isSos; }
    public void setIsSos(Boolean isSos) { this.isSos = isSos; }
    public String getAcks() { return acks; }
    public void setAcks(String acks) { this.acks = acks; }
    public LocalDateTime getCreateTime() { return createTime; }
    public void setCreateTime(LocalDateTime createTime) { this.createTime = createTime; }
}