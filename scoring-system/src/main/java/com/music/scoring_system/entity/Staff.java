package com.music.scoring_system.entity;

import jakarta.persistence.*;

@Entity
@Table(name = "sys_staff")
public class Staff {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    @Column(length = 20)
    private String studentNo;

    @Column(nullable = false)
    private String role; // 归属组别，例如 "舞台组"

    @Column(nullable = false)
    private Integer level; // 2=二级负责人, 3=三级执行员

    // 🚀 新增：安全与审核字段
    private String password;

    // 状态："PENDING" (待审核) 或 "APPROVED" (已通过)
    @Column(columnDefinition = "varchar(20) default 'PENDING'")
    private String status;

    // 是否需要强制重置密码
    @Column(name = "require_pwd_change", columnDefinition = "boolean default false")
    private Boolean requirePasswordChange;

    // --- 终端特权配置字段 ---
    @Column(columnDefinition = "boolean default false")
    private Boolean canGlobalBroadcast = false;

    @Column(columnDefinition = "boolean default true")
    private Boolean canGroupBroadcast = true;

    @Column(columnDefinition = "boolean default true")
    private Boolean canSOS = true;

    @Column(columnDefinition = "boolean default false")
    private Boolean canDirectReport = false;

    @Column(columnDefinition = "boolean default false")
    private Boolean managePower = false;

    @Column(columnDefinition = "boolean default false")
    private Boolean silentMode = false;

    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getStudentNo() { return studentNo; }
    public void setStudentNo(String studentNo) { this.studentNo = studentNo; }

    public String getRole() { return role; }
    public void setRole(String role) { this.role = role; }

    public Integer getLevel() { return level; }
    public void setLevel(Integer level) { this.level = level; }

    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public Boolean getRequirePasswordChange() { return requirePasswordChange; }
    public void setRequirePasswordChange(Boolean requirePasswordChange) { this.requirePasswordChange = requirePasswordChange; }

    public Boolean getCanGlobalBroadcast() { return canGlobalBroadcast; }
    public void setCanGlobalBroadcast(Boolean canGlobalBroadcast) { this.canGlobalBroadcast = canGlobalBroadcast; }

    public Boolean getCanGroupBroadcast() { return canGroupBroadcast; }
    public void setCanGroupBroadcast(Boolean canGroupBroadcast) { this.canGroupBroadcast = canGroupBroadcast; }

    public Boolean getCanSOS() { return canSOS; }
    public void setCanSOS(Boolean canSOS) { this.canSOS = canSOS; }

    public Boolean getCanDirectReport() { return canDirectReport; }
    public void setCanDirectReport(Boolean canDirectReport) { this.canDirectReport = canDirectReport; }

    public Boolean getManagePower() { return managePower; }
    public void setManagePower(Boolean managePower) { this.managePower = managePower; }

    public Boolean getSilentMode() { return silentMode; }
    public void setSilentMode(Boolean silentMode) { this.silentMode = silentMode; }
}