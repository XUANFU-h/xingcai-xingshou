package com.music.scoring_system.controller;
import com.music.scoring_system.entity.StudentRep;
import com.music.scoring_system.repository.StudentRepRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/students")
@CrossOrigin
public class StudentRepController {

    @Autowired
    private StudentRepRepository repo;

    @GetMapping
    public List<StudentRep> getAll() { return repo.findAll(); }

    @PostMapping
    public StudentRep addStudent(@RequestBody StudentRep student) { return repo.save(student); }

    @DeleteMapping("/{id}")
    public void deleteStudent(@PathVariable Integer id) { repo.deleteById(id); }
}