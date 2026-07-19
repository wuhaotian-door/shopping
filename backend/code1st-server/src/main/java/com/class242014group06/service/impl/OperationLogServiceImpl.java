package com.class242014group06.service.impl;

import com.class242014group06.entity.OperationLog;
import com.class242014group06.mapper.OperationLogMapper;
import com.class242014group06.service.OperationLogService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@Slf4j
public class OperationLogServiceImpl implements OperationLogService {

    @Autowired
    private OperationLogMapper operationLogMapper;

    @Override
    public void log(Long operatorId, String operatorName, String operatorRole,
                    String operationType, String targetType, Long targetId, String detail) {
        try {
            OperationLog logEntry = OperationLog.builder()
                    .operatorId(operatorId)
                    .operatorName(operatorName)
                    .operatorRole(operatorRole)
                    .operationType(operationType)
                    .targetType(targetType)
                    .targetId(targetId)
                    .detail(detail)
                    .build();
            operationLogMapper.insert(logEntry);
        } catch (Exception e) {
            // 日志记录失败不应影响主业务
            log.warn("操作日志记录失败: {}", e.getMessage());
        }
    }

    @Override
    public List<OperationLog> listRecent() {
        return operationLogMapper.listRecent();
    }

    @Override
    public List<OperationLog> listByOperator(Long operatorId) {
        return operationLogMapper.listByOperator(operatorId);
    }

    @Override
    public List<OperationLog> listByType(String type) {
        return operationLogMapper.listByType(type);
    }
}
