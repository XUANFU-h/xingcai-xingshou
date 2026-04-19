package com.music.scoring_system.controller;

import com.music.scoring_system.entity.Judge;
import com.music.scoring_system.repository.JudgeRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/judges")
@CrossOrigin
public class JudgeController {

    @Autowired
    private JudgeRepository judgeRepository;

    // 获取所有评委
    @GetMapping
    public List<Judge> getAllJudges() {
        return judgeRepository.findAll();
    }

    // 新增/修改评委
    @PostMapping
    public Judge saveJudge(@RequestBody Judge judge) {
        return judgeRepository.save(judge);
    }

    // 删除评委
    @DeleteMapping("/{id}")
    public ResponseEntity<String> deleteJudge(@PathVariable Integer id) {
        judgeRepository.deleteById(id);
        return ResponseEntity.ok("评委删除成功");
    }
}