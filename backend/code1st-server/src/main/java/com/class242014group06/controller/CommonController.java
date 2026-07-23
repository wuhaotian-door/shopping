package com.class242014group06.controller;

import com.class242014group06.constant.MessageConstant;
import com.class242014group06.result.Result;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.util.Set;
import java.util.UUID;

/**
 * 通用接口（文件上传等）
 */
@RestController
@RequestMapping("/admin/common")
@Slf4j
@Api(tags = "通用接口")
public class CommonController {

    /** 允许的图片类型 */
    private static final Set<String> ALLOWED_EXTENSIONS = Set.of("jpg", "jpeg", "png", "gif", "webp");

    /** 最大文件大小（字节），默认 10MB */
    @Value("${code1st.upload.max-size:10485760}")
    private long maxFileSize;

    /** 上传目录 */
    @Value("${code1st.upload.path:./uploads}")
    private String uploadPath;

    /**
     * 上传文件（图片）
     * 返回可访问的 URL 路径
     */
    @PostMapping("/upload")
    @ApiOperation("上传文件")
    public Result<String> upload(@RequestParam("file") MultipartFile file) {
        // 1. 文件非空校验
        if (file == null || file.isEmpty()) {
            return Result.error(MessageConstant.FILE_EMPTY);
        }

        // 2. 文件大小校验
        if (file.getSize() > maxFileSize) {
            return Result.error(MessageConstant.FILE_SIZE_EXCEEDED);
        }

        // 3. 文件类型校验
        String originalFilename = file.getOriginalFilename();
        String extension = "";
        if (originalFilename != null && originalFilename.contains(".")) {
            extension = originalFilename.substring(originalFilename.lastIndexOf(".") + 1).toLowerCase();
        }
        if (!ALLOWED_EXTENSIONS.contains(extension)) {
            return Result.error(MessageConstant.FILE_TYPE_INVALID);
        }

        // 4. 生成唯一文件名并保存
        String newFilename = UUID.randomUUID().toString() + "." + extension;

        try {
            // 转为绝对路径，避免 Tomcat 把相对路径解析到临时目录
            File baseDir = new File(uploadPath).getAbsoluteFile();
            if (!baseDir.exists()) {
                baseDir.mkdirs();
            }

            File destFile = new File(baseDir, newFilename);
            file.transferTo(destFile);

            String url = "/uploads/" + newFilename;
            log.info("文件上传成功：{} -> {}", originalFilename, url);
            return Result.success(url);
        } catch (IOException e) {
            log.error("文件上传失败", e);
            return Result.error(MessageConstant.UPLOAD_FAILED);
        }
    }
}
