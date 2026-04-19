package com.music.scoring_system.repository;

import com.music.scoring_system.entity.SystemLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface SystemLogRepository extends JpaRepository<SystemLog, Long> {
    // 拉取最新的 500 条系统日志
    List<SystemLog> findTop500ByOrderByDbIdDesc();
}