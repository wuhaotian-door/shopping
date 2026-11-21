package com.class242014group06.handler;


import com.class242014group06.constant.MessageConstant;
import com.class242014group06.exception.BaseException;
import com.class242014group06.exception.ProductCatalogBusyException;
import com.class242014group06.result.Result;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.multipart.MultipartException;

import java.sql.SQLIntegrityConstraintViolationException;

/**
 * 全局异常处理器，处理项目中抛出的业务异常
 */
@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    @ExceptionHandler(ProductCatalogBusyException.class)
    public ResponseEntity<Result<Void>> handleProductCatalogBusy(ProductCatalogBusyException ex) {
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                .header(HttpHeaders.RETRY_AFTER, "1")
                .body(Result.error(ex.getMessage()));
    }

    /**
     * 捕获业务异常
     * @param ex
     * @return
     */
    @ExceptionHandler
    public Result exceptionHandler(BaseException ex){
        log.error("异常信息：{}", ex.getMessage());
        return Result.error(ex.getMessage());
    }

    /**
     * 处理 SQL 唯一约束冲突
     * @param ex
     * @return
     */
    @ExceptionHandler(SQLIntegrityConstraintViolationException.class)
    public Result exceptionHandler(SQLIntegrityConstraintViolationException ex){
        // Duplicate entry 'zhangsan' for key 'xxx.idx_xxx_username'
        String message = ex.getMessage();
        if(message.contains("Duplicate entry") && message.contains("username")){
            String[] split = message.split(" ");
            String username = split[2];
            String msg = username + MessageConstant.ALREADY_EXISTS;
            return Result.error(msg);
        } else {
            return Result.error(MessageConstant.UNKNOWN_ERROR);
        }
    }

    /**
     * 处理文件上传超过大小限制 — Spring 在请求进入 Controller 前抛出
     */
    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public Result exceptionHandler(MaxUploadSizeExceededException ex) {
        log.warn("文件上传超限：{}", ex.getMessage());
        return Result.error(MessageConstant.FILE_SIZE_EXCEEDED);
    }

    /**
     * 处理其他 Multipart 异常（如请求不是 multipart、缺少 boundary 等）
     */
    @ExceptionHandler(MultipartException.class)
    public Result exceptionHandler(MultipartException ex) {
        log.warn("文件上传异常：{}", ex.getMessage());
        return Result.error("文件上传失败：" + ex.getMessage());
    }

    /**
     * 兜底异常处理 — 防止未预期的异常直接返回 500 或空响应
     */
    @ExceptionHandler(Exception.class)
    public Result exceptionHandler(Exception ex) {
        log.error("未预期的异常：", ex);
        return Result.error("请求失败: " + ex.getMessage());
    }
}
