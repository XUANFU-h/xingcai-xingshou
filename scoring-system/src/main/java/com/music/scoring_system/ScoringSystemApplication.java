package com.music.scoring_system;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling // 🚀 核心新增：开启 Spring Boot 底层定时任务引擎
public class ScoringSystemApplication {

	public static void main(String[] args) {
		SpringApplication.run(ScoringSystemApplication.class, args);
	}

}