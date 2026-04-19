package com.music.scoring_system.controller;

import com.music.scoring_system.entity.Contestant;
import com.music.scoring_system.repository.ContestantRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/admin/contestants")
@CrossOrigin
public class AdminController {

    @Autowired
    private ContestantRepository contestantRepository;

    // 🌟 注入 Redis 模板
    @Autowired
    private StringRedisTemplate redisTemplate;

    // 🌟 获取当前赛段的辅助方法
    private String getCurrentSession() {
        String val = redisTemplate.opsForValue().get("sys:currentSession");
        return val != null ? val : "默认赛段";
    }

    // 1. 获取【当前赛段】的所有选手
    @GetMapping
    public List<Contestant> getAllContestants() {
        return contestantRepository.findBySessionName(getCurrentSession());
    }

    // 2. 添加或更新选手信息
    @PostMapping
    public ResponseEntity<Contestant> saveContestant(@RequestBody Contestant contestant) {
        if (contestant.getId() == null || contestant.getSessionName() == null) {
            contestant.setSessionName(getCurrentSession());
        }
        Contestant savedContestant = contestantRepository.save(contestant);
        return ResponseEntity.ok(savedContestant);
    }

    // 3. 删除选手
    @DeleteMapping("/{id}")
    public ResponseEntity<String> deleteContestant(@PathVariable Integer id) {
        contestantRepository.deleteById(id);
        return ResponseEntity.ok("选手删除成功！");
    }

    // 👉 新增：批量更新选手排序接口 (已修复 Lambda 变量作用域问题)
    @PostMapping("/reorder")
    public ResponseEntity<String> reorderContestants(@RequestBody List<Integer> ids) {
        List<Contestant> all = contestantRepository.findBySessionName(getCurrentSession());

        for (int i = 0; i < ids.size(); i++) {
            Integer currentId = ids.get(i);
            // 改用传统的 for 循环查找并更新，完美避开 Java Lambda 的 effectively final 限制
            for (Contestant c : all) {
                if (c.getId().equals(currentId)) {
                    c.setSortOrder(i);
                    break;
                }
            }
        }

        contestantRepository.saveAll(all); // 批量保存新顺序
        return ResponseEntity.ok("顺序已更新");
    }
}