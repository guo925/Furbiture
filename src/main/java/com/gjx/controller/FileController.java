package com.gjx.controller;

import com.aliyun.oss.OSS;
import com.aliyun.oss.OSSClientBuilder;
import com.gjx.common.R;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;
import java.util.UUID;

/**
 * 文件上传控制器
 */
@RestController
@RequestMapping("/api/files")
public class FileController {

    @Value("${oss.endpoint}")
    private String endpoint;

    @Value("${oss.bucket-name}")
    private String bucketName;

    @Value("${oss.access-key-id}")
    private String accessKeyId;

    @Value("${oss.access-key-secret}")
    private String accessKeySecret;

    /**
     * 上传文件
     * @param file 要上传的文件
     * @return 上传结果
     */
    @PostMapping("/upload")
    public R<?> upload(@RequestParam("file") MultipartFile file) {
        System.out.println("========== 开始处理文件上传 ==========");
        System.out.println("文件信息: " + file.getOriginalFilename() + ", 大小: " + file.getSize());
        System.out.println("OSS配置 - endpoint: " + endpoint + ", bucket: " + bucketName);
        System.out.println("Access Key ID: " + accessKeyId);

        if (file.isEmpty()) {
            System.out.println("文件为空");
            return R.error("请选择要上传的文件");
        }

        OSS ossClient = null;
        try {
            // 生成唯一文件名
            String originalFilename = file.getOriginalFilename();
            String suffix = originalFilename.substring(originalFilename.lastIndexOf("."));
            String fileName = "uploads/" + UUID.randomUUID().toString() + suffix;
            System.out.println("生成的文件名: " + fileName);

            // 初始化OSS客户端
            System.out.println("初始化OSS客户端...");
            ossClient = new OSSClientBuilder().build(endpoint, accessKeyId, accessKeySecret);
            System.out.println("OSS客户端初始化成功");

            // 获取文件输入流
            try (InputStream inputStream = file.getInputStream()) {
                System.out.println("开始上传文件到OSS...");
                // 上传文件到OSS
                ossClient.putObject(bucketName, fileName, inputStream);
                System.out.println("文件上传成功");
            }

            // 生成文件URL
            String fileUrl = "https://" + bucketName + ".oss-cn-beijing.aliyuncs.com/" + fileName;
            System.out.println("生成的URL: " + fileUrl);
            System.out.println("========== 文件上传成功 ==========");

            return R.ok(fileUrl);
        } catch (Exception e) {
            System.out.println("========== 文件上传失败 ==========");
            e.printStackTrace();
            System.out.println("错误信息: " + e.getMessage());
            return R.error("文件上传失败: " + e.getMessage());
        } finally {
            // 关闭OSS客户端
            if (ossClient != null) {
                ossClient.shutdown();
            }
        }
    }
}
