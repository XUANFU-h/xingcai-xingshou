package com.music.scoring_system.repository;

import com.music.scoring_system.entity.ChatMessage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface ChatMessageRepository extends JpaRepository<ChatMessage, Long> {
    // 拉取最新的 500 条聊天记录（防止数据量过大卡死终端）
    List<ChatMessage> findTop500ByOrderByDbIdDesc();

    // 用于处理 SOS 确认回执的查询
    ChatMessage findFirstByContentAndIsSosTrueOrderByDbIdDesc(String content);
}