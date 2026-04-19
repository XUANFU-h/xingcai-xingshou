package com.music.scoring_system.entity;

import jakarta.persistence.*;

@Entity // 告诉 Spring Boot 这是一个数据库表映射类
@Table(name = "contestants") // 指定表名为 contestants
public class Contestant {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id; // 选手的唯一 ID（自动递增）

    @Column(nullable = false, length = 50)
    private String name; // 姓名

    @Column(name = "class_name", length = 100)
    private String className; // 班级（比如：软件技术 24级1班）

    @Column(name = "photo_url", length = 255)
    private String photoUrl; // 照片的存放路径

    @Column(name = "final_score")
    private Double finalScore = 0.00;

    @Column(name = "judge_avg_score")
    private Double judgeAvgScore = 0.0; // 专业评委平均分

    @Column(name = "student_avg_score")
    private Double studentAvgScore = 0.0; // 学生代表平均分

    @Column(name = "song_name", length = 100)
    private String songName; // 演唱歌曲

    private String sessionName;

    // 👉 新增：用于自定义排序的序号字段（默认999，确保新加入的选手排在末尾）
    @Column(name = "sort_order")
    private Integer sortOrder = 999;

    // --- 必须生成所有的 Getters 和 Setters ---
    public Integer getId() { return id; }
    public void setId(Integer id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getClassName() { return className; }
    public void setClassName(String className) { this.className = className; }

    public String getPhotoUrl() { return photoUrl; }
    public void setPhotoUrl(String photoUrl) { this.photoUrl = photoUrl; }

    public Double getFinalScore() { return finalScore; }
    public void setFinalScore(Double finalScore) { this.finalScore = finalScore; }

    public String getSongName() { return songName; }
    public void setSongName(String songName) { this.songName = songName; }

    public Double getJudgeAvgScore() { return judgeAvgScore; }
    public void setJudgeAvgScore(Double judgeAvgScore) { this.judgeAvgScore = judgeAvgScore; }

    public Double getStudentAvgScore() { return studentAvgScore; }
    public void setStudentAvgScore(Double studentAvgScore) { this.studentAvgScore = studentAvgScore; }

    public String getSessionName() { return sessionName; }
    public void setSessionName(String sessionName) { this.sessionName = sessionName; }

    public Integer getSortOrder() { return sortOrder; }
    public void setSortOrder(Integer sortOrder) { this.sortOrder = sortOrder; }
}