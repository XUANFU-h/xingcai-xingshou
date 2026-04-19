package com.music.scoring_system.task;

import com.music.scoring_system.config.ScoreWebSocketHandler;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class SystemSecurityTask {

    @Autowired
    private StringRedisTemplate redisTemplate;

    @Autowired
    private ScoreWebSocketHandler webSocketHandler;

    // 4小时 = 4 * 60 * 60 * 1000 毫秒
    private static final long TIMEOUT_MS = 4 * 60 * 60 * 1000L;

    // 🚀 每分钟执行一次巡检，检测系统是否处于长时静默状态
    @Scheduled(fixedRate = 60000)
    public void checkSystemInactivity() {
        String lastActiveStr = redisTemplate.opsForValue().get("sys:last_admin_active_time");
        long currentTime = System.currentTimeMillis();

        if (lastActiveStr == null) {
            redisTemplate.opsForValue().set("sys:last_admin_active_time", String.valueOf(currentTime));
            return;
        }

        long lastActiveTime = Long.parseLong(lastActiveStr);
        if (currentTime - lastActiveTime > TIMEOUT_MS) {
            System.out.println("🚨 触发 4 小时死间熔断机制！系统进入强制锁死状态！");
            executeEmergencyLockdown();
            redisTemplate.opsForValue().set("sys:last_admin_active_time", String.valueOf(currentTime));
        }
    }

    // 🚀 每天凌晨 4:00 执行 24 小时硬性覆盖重置
    @Scheduled(cron = "0 0 4 * * ?")
    public void dailySystemReset() {
        System.out.println("☢️ 触发 24 小时凌晨系统重置指令！");
        executeEmergencyLockdown();
        redisTemplate.opsForValue().set("sys:last_admin_active_time", String.valueOf(System.currentTimeMillis()));
    }

    // ⚔️ 核心熔断逻辑：清空关键配置并切断通讯
    private void executeEmergencyLockdown() {
        // 关闭投票通道
        redisTemplate.opsForValue().set("sys:isVotingOpen", "false");
        // 清空台上选手
        redisTemplate.delete("sys:activeContestantId");
        // 清空大众端在线数据
        redisTemplate.delete("sys:active_audience_zset");
        redisTemplate.delete("sys:activeDevices");

        // 广播死间锁死信号，所有终端（导播、评委、大众、大屏）无条件退回登录锁屏
        webSocketHandler.broadcastScore("{\"type\": \"SYSTEM_LOCKED\"}");
    }
}