package com.music.scoring_system.entity;

import jakarta.persistence.*;

@Entity
@Table(name = "student_reps")
public class StudentRep {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;
    private String name;

    public Integer getId() { return id; }
    public void setId(Integer id) { this.id = id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
}