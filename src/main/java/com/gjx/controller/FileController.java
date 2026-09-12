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
import java.util.Locale;
import java.util.Set;
import java.util.UUID;

/**
 * 文件上传控制器
 */
@Slf4j
@RestController
@RequestMapping("/api/files")
public class FileController {

    /**
     * 允许上传的文件扩展名白名单
     * <p>
     * 不做类型限制时，攻击者可上传 .html / .svg / .jsp 等文件，
     * 配合 /uploads/** 静态资源映射形成存储型 XSS 甚至更严重的风险。
     */
    private static final Set<String> ALLOWED_EXTENSIONS =
            Set.of(".jpg", ".jpeg", ".png", ".gif", ".webp", ".bmp");

    /**
     * 允许上传的 MIME 类型白名单（与扩展名双重校验，扩展名可伪造）
     */
    private static final Set<String> ALLOWED_CONTENT_TYPES =
            Set.of("image/jpeg", "image/png", "image/gif", "image/webp", "image/bmp");

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
     * OSS 访问域名前缀，例如 https://bucket.oss-cn-beijing.aliyuncs.com
     * 未配置时根据 endpoint 推导，避免把 region 写死在代码里
     */
    @Value("${oss.url-prefix:}")
    private String ossUrlPrefix;

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

        String suffix = getFileSuffix(file.getOriginalFilename());
        if (!ALLOWED_EXTENSIONS.contains(suffix)) {
            return R.error("仅支持上传 " + String.join("/", ALLOWED_EXTENSIONS) + " 格式的图片");
        }
        // 必须先判 null：Set.of(...) 生成的是不可变集合，其 contains(null) 会抛 NPE，
        // 而 multipart 分片未携带 Content-Type 时 getContentType() 恰好返回 null
        String contentType = file.getContentType();
        if (contentType == null || !ALLOWED_CONTENT_TYPES.contains(contentType)) {
            return R.error("文件内容类型不合法");
        }

        try {
            String fileName = UUID.randomUUID() + suffix;

            if (hasOssConfig()) {
                return uploadToOss(file, fileName);
            }

            return uploadToLocal(file, fileName);
        } catch (Exception e) {
            // 只记录服务端日志，对外返回通用提示，避免泄露内部路径等实现细节
            log.error("[文件上传失败] fileName={}, error={}", file.getOriginalFilename(), e.getMessage(), e);
            throw new BusinessException("文件上传失败，请稍后重试");
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
            return R.ok(resolveOssUrlPrefix() + "/" + objectName);
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

    /**
     * 解析 OSS 访问地址前缀：优先使用显式配置，否则根据 endpoint 推导
     */
    private String resolveOssUrlPrefix() {
        if (hasText(ossUrlPrefix)) {
            return ossUrlPrefix.endsWith("/") ? ossUrlPrefix.substring(0, ossUrlPrefix.length() - 1) : ossUrlPrefix;
        }
        String host = endpoint.replaceFirst("^https?://", "");
        return "https://" + bucketName + "." + host;
    }

    /**
     * 提取文件扩展名并统一转小写，便于白名单比对
     */
    private String getFileSuffix(String originalFilename) {
        if (originalFilename == null) {
            return "";
        }
        int dotIndex = originalFilename.lastIndexOf(".");
        if (dotIndex < 0 || dotIndex == originalFilename.length() - 1) {
            return "";
        }
        return originalFilename.substring(dotIndex).toLowerCase(Locale.ROOT);
    }
}
