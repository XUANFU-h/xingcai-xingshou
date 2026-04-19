package com.music.scoring_system.repository;
import com.music.scoring_system.entity.StudentRep;
import org.springframework.data.jpa.repository.JpaRepository;

public interface StudentRepRepository extends JpaRepository<StudentRep, Integer> {
}