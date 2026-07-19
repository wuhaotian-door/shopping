package com.class242014group06.service;

import com.class242014group06.entity.OperationLog;

import java.util.List;

/**
 * 管理员操作日志服务
 */
public interface OperationLogService {

    /**
     * 记录一条操作日志
     */
    void log(Long operatorId, String operatorName, String operatorRole,
             String operationType, String targetType, Long targetId, String detail);

    /**
     * 查询最近的操作日志
     */
    List<OperationLog> listRecent();

    /**
     * 按操作人查询
     */
    List<OperationLog> listByOperator(Long operatorId);

    /**
     * 按操作类型查询
     */
    List<OperationLog> listByType(String type);
}
