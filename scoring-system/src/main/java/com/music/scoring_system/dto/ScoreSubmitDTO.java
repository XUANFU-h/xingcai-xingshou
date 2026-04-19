package com.music.scoring_system.dto;

public class ScoreSubmitDTO {
    private Integer contestantId; // 选手ID
    private String judgeName;     // 评委名字
    private Double score;         // 分数
    private String judgeType;     // 打分人身份标识 ("JUDGE" 或 "STUDENT")

    // 【安全更新】：用于防刷票和口令验证
    private String deviceId;
    private String pin;

    // Getters 和 Setters
    public Integer getContestantId() { return contestantId; }
    public void setContestantId(Integer contestantId) { this.contestantId = contestantId; }
    public String getJudgeName() { return judgeName; }
    public void setJudgeName(String judgeName) { this.judgeName = judgeName; }
    public Double getScore() { return score; }
    public void setScore(Double score) { this.score = score; }
    public String getJudgeType() { return judgeType; }
    public void setJudgeType(String judgeType) { this.judgeType = judgeType; }
    public String getDeviceId() { return deviceId; }
    public void setDeviceId(String deviceId) { this.deviceId = deviceId; }
    public String getPin() { return pin; }
    public void setPin(String pin) { this.pin = pin; }
}