package com.music.scoring_system.repository;

import com.music.scoring_system.entity.Staff;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface StaffRepository extends JpaRepository<Staff, Long> {

    // 按名字查询
    Optional<Staff> findByName(String name);

    // 🚀 新增：按名字和学号精准查询（用于登录核验）
    Optional<Staff> findByNameAndStudentNo(String name, String studentNo);

    // 🚀 新增：按状态查询（用于拉取已通过/待审批名单）
    List<Staff> findByStatus(String status);
}