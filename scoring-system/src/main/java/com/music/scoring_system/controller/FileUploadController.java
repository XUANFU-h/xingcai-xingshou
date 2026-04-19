package com.music.scoring_system.controller;

import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.UUID;

@RestController
@CrossOrigin
public class FileUploadController {

    private final String UPLOAD_DIR = "/home/scoring_uploads/";

    // 🚀 终极大招：换一个完全独立、没有任何歧义的接口路径，并使用 RequestMapping 接收所有方法
    @RequestMapping(value = "/system-direct-upload", method = {RequestMethod.POST, RequestMethod.PUT})
    public ResponseEntity<String> uploadFile(@RequestParam("file") MultipartFile file) {
        if (file.isEmpty()) return ResponseEntity.badRequest().body("文件为空");
        try {
            File dir = new File(UPLOAD_DIR);
            if (!dir.exists()) dir.mkdirs();

            String originalFilename = file.getOriginalFilename();
            String extension = originalFilename.substring(originalFilename.lastIndexOf("."));
            String newFilename = UUID.randomUUID().toString() + extension;

            File dest = new File(UPLOAD_DIR + newFilename);
            Files.copy(file.getInputStream(), dest.toPath(), StandardCopyOption.REPLACE_EXISTING);

            return ResponseEntity.ok("/uploads/" + newFilename);
        } catch (IOException e) {
            e.printStackTrace();
            return ResponseEntity.status(500).body("图片上传失败: " + e.getMessage());
        }
    }

    // 用于测试接口是否存活的小彩蛋
    @GetMapping("/system-direct-upload")
    public ResponseEntity<String> testUploadApi() {
        return ResponseEntity.ok("上传接口存活，但请使用 POST 发送文件！");
    }

    @GetMapping("/uploads/{filename}")
    public ResponseEntity<Resource> getImage(@PathVariable String filename) {
        File file = new File(UPLOAD_DIR + filename);
        if (!file.exists()) return ResponseEntity.notFound().build();

        MediaType mediaType = MediaType.IMAGE_JPEG;
        String lowerName = filename.toLowerCase();
        if (lowerName.endsWith(".png")) mediaType = MediaType.IMAGE_PNG;
        else if (lowerName.endsWith(".gif")) mediaType = MediaType.IMAGE_GIF;

        return ResponseEntity.ok().contentType(mediaType).body(new FileSystemResource(file));
    }
}