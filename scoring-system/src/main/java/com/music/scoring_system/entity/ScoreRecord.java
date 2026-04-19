package com.music.scoring_system.entity;

import jakarta.persistence.*;

@Entity
@Table(name = "score_records")
public class ScoreRecord {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;
    private Integer contestantId; // 选手ID
    private String judgeName;     // 评委名字
    private Double scoreValue;    // 打出的分数
    @Column(name = "judge_type")
    private String judgeType; // 身份标识："JUDGE" (评委) 或 "STUDENT" (学生)
    private String sessionName;

    // 【安全更新】：设备硬件指纹，防止重复刷票
    private String deviceId;

    // --- Getters 和 Setters ---
    public Integer getId() { return id; }
    public void setId(Integer id) { this.id = id; }
    public Integer getContestantId() { return contestantId; }
    public void setContestantId(Integer contestantId) { this.contestantId = contestantId; }
    public String getJudgeName() { return judgeName; }
    public void setJudgeName(String judgeName) { this.judgeName = judgeName; }
    public Double getScoreValue() { return scoreValue; }
    public void setScoreValue(Double scoreValue) { this.scoreValue = scoreValue; }
    public String getJudgeType() { return judgeType; }
    public void setJudgeType(String judgeType) { this.judgeType = judgeType; }
    public String getSessionName() { return sessionName; }
    public void setSessionName(String sessionName) { this.sessionName = sessionName; }
    public String getDeviceId() { return deviceId; }
    public void setDeviceId(String deviceId) { this.deviceId = deviceId; }
}