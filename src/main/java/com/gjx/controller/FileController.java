package com.gjx.controller;

import com.aliyun.oss.OSS;
import com.aliyun.oss.OSSClientBuilder;
import com.gjx.common.BusinessException;
import com.gjx.common.R;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.UUID;

/**
 * 文件上传控制器
 */
@Slf4j
@RestController
@RequestMapping("/api/files")
public class FileController {

    @Value("${oss.endpoint:}")
    private String endpoint;

    @Value("${oss.bucket-name:}")
    private String bucketName;

    @Value("${oss.access-key-id:}")
    private String accessKeyId;

    @Value("${oss.access-key-secret:}")
    private String accessKeySecret;

    @Value("${file.upload-dir:/tmp/furniture-uploads/}")
    private String uploadDir;

    /**
     * 上传文件
     * @param file 要上传的文件
     * @return 上传结果
     */
    @PostMapping("/upload")
    public R<?> upload(@RequestParam("file") MultipartFile file) {
        if (file.isEmpty()) {
            return R.error("请选择要上传的文件");
        }

        try {
            String originalFilename = file.getOriginalFilename();
            String suffix = getFileSuffix(originalFilename);
            String fileName = UUID.randomUUID() + suffix;

            if (hasOssConfig()) {
                return uploadToOss(file, fileName);
            }

            return uploadToLocal(file, fileName);
        } catch (Exception e) {
            log.error("[文件上传失败] fileName={}, error={}", file.getOriginalFilename(), e.getMessage(), e);
            throw new BusinessException("文件上传失败: " + e.getMessage());
        }
    }

    private R<?> uploadToOss(MultipartFile file, String fileName) throws Exception {
        OSS ossClient = null;
        String objectName = "uploads/" + fileName;
        try {
            ossClient = new OSSClientBuilder().build(endpoint, accessKeyId, accessKeySecret);
            try (InputStream inputStream = file.getInputStream()) {
                ossClient.putObject(bucketName, objectName, inputStream);
            }
            return R.ok("https://" + bucketName + ".oss-cn-beijing.aliyuncs.com/" + objectName);
        } finally {
            if (ossClient != null) {
                ossClient.shutdown();
            }
        }
    }

    private R<?> uploadToLocal(MultipartFile file, String fileName) throws Exception {
        Path uploadPath = Paths.get(uploadDir).toAbsolutePath().normalize();
        Files.createDirectories(uploadPath);

        Path target = uploadPath.resolve(fileName).normalize();
        if (!target.startsWith(uploadPath)) {
            return R.error("非法文件名");
        }

        file.transferTo(target);
        return R.ok("/uploads/" + fileName);
    }

    private boolean hasOssConfig() {
        return hasText(endpoint) && hasText(bucketName) && hasText(accessKeyId) && hasText(accessKeySecret);
    }

    private boolean hasText(String value) {
        return value != null && !value.trim().isEmpty();
    }

    private String getFileSuffix(String originalFilename) {
        if (originalFilename == null) {
            return "";
        }
        int dotIndex = originalFilename.lastIndexOf(".");
        if (dotIndex < 0 || dotIndex == originalFilename.length() - 1) {
            return "";
        }
        return originalFilename.substring(dotIndex);
    }
}
