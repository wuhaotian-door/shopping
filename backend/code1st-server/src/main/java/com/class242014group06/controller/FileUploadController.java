package com.class242014group06.controller;

import com.class242014group06.result.Result;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;

/**
 * 通用文件上传控制器
 * 自动区分图片 / 3D模型类型，分类存储到 static 目录下
 */
@RestController
@RequestMapping("/admin/upload")
@Slf4j
public class FileUploadController {

    /** 静态资源根目录，对应 application.yml 中 code1st.static.path */
    @Value("${code1st.static.path:./static}")
    private String staticPath;

    /** 允许的图片格式 */
    private static final Set<String> IMAGE_EXTENSIONS = Set.of("jpg", "jpeg", "png", "webp");

    /** 允许的模型格式 */
    private static final Set<String> MODEL_EXTENSIONS = Set.of("glb", "gltf");

    /** 允许的视频格式 */
    private static final Set<String> VIDEO_EXTENSIONS = Set.of("mp4", "webm", "mov");

    /** 图片最大大小：5MB */
    private static final long IMAGE_MAX_SIZE = 5 * 1024 * 1024;

    /** 模型最大大小：20MB */
    private static final long MODEL_MAX_SIZE = 20 * 1024 * 1024;

    /** 视频最大大小：50MB */
    private static final long VIDEO_MAX_SIZE = 50 * 1024 * 1024;

    /**
     * 通用文件上传接口
     *
     * @param file 上传的文件
     * @param type 文件类型：image 或 model（用于区分存储目录）
     * @return Result.data = 相对访问路径（如 /images/xxx.jpg）
     */
    @PostMapping
    public Result<String> upload(
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "type", defaultValue = "image") String type) {

        // 1. 基础校验
        if (file == null || file.isEmpty()) {
            return Result.error("上传文件不能为空");
        }

        String originalFilename = file.getOriginalFilename();
        if (originalFilename == null || !originalFilename.contains(".")) {
            return Result.error("文件格式无法识别");
        }

        // 2. 获取扩展名（小写）
        String extension = originalFilename.substring(originalFilename.lastIndexOf('.') + 1).toLowerCase();

        // 3. 根据类型进行格式和大小校验
        String subDir; // 存储子目录
        Set<String> allowedExtensions;
        long maxSize;

        if ("model".equals(type)) {
            subDir = "models";
            allowedExtensions = MODEL_EXTENSIONS;
            maxSize = MODEL_MAX_SIZE;
        } else if ("video".equals(type)) {
            subDir = "videos";
            allowedExtensions = VIDEO_EXTENSIONS;
            maxSize = VIDEO_MAX_SIZE;
        } else {
            // 默认走图片逻辑
            subDir = "images";
            allowedExtensions = IMAGE_EXTENSIONS;
            maxSize = IMAGE_MAX_SIZE;
        }

        // 格式校验
        if (!allowedExtensions.contains(extension)) {
            String expected = String.join("/", allowedExtensions);
            return Result.error("文件格式不支持，仅允许 " + expected + " 格式");
        }

        // 大小校验
        if (file.getSize() > maxSize) {
            String sizeLabel = maxSize >= 1024 * 1024 ? (maxSize / 1024 / 1024) + "MB" : (maxSize / 1024) + "KB";
            return Result.error("文件大小超限，最大允许 " + sizeLabel);
        }

        // 4. 生成唯一文件名（UUID + 原扩展名）
        String newFilename = UUID.randomUUID().toString().replace("-", "") + "." + extension;

        // 5. 确保目标目录存在
        try {
            // 解析为绝对路径，避免相对路径受 Tomcat 工作目录影响
            String absoluteStaticPath = new File(staticPath).getAbsolutePath();
            Path targetDir = Paths.get(absoluteStaticPath, subDir);
            Files.createDirectories(targetDir);

            // 6. 写入文件
            Path targetFile = targetDir.resolve(newFilename);
            file.transferTo(targetFile.toFile());

            // 7. 返回相对访问路径（前端可直接用于 <img src> 或 model-viewer src）
            String accessPath = "/" + subDir + "/" + newFilename;
            log.info("文件上传成功：{} -> {} ({} bytes)", originalFilename, accessPath, file.getSize());

            return Result.success(accessPath);

        } catch (IOException e) {
            log.error("文件上传失败：{}", originalFilename, e);
            return Result.error("文件上传失败，请稍后重试");
        }
    }
}
