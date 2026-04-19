package com.music.scoring_system.repository;

import com.music.scoring_system.entity.Contestant;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ContestantRepository extends JpaRepository<Contestant, Integer> {
    List<Contestant> findBySessionName(String sessionName);
    // 只要继承了 JpaRepository，Spring Boot 就会自动帮你实现
    // save(), findAll(), findById(), deleteById() 等所有基础方法！
}