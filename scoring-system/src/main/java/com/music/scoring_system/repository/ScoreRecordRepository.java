package com.music.scoring_system.repository;

import com.music.scoring_system.entity.ScoreRecord;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;

@Repository
public interface ScoreRecordRepository extends JpaRepository<ScoreRecord, Integer> {
    List<ScoreRecord> findByContestantId(Integer contestantId);

    // 【安全更新】：快速查询该设备是否已经对该选手投过票！
    boolean existsByContestantIdAndDeviceId(Integer contestantId, String deviceId);

    @Transactional
    void deleteByContestantId(Integer contestantId);
    @Transactional
    void deleteBySessionName(String sessionName);
}