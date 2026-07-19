package com.class242014group06.mapper;

import com.class242014group06.annotation.AutoFill;
import com.class242014group06.entity.OperationLog;
import com.class242014group06.enumeration.OperationType;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface OperationLogMapper {

    @AutoFill(value = OperationType.INSERT)
    @Insert("insert into admin_operation_log (operator_id, operator_name, operator_role, operation_type, target_type, target_id, detail, create_time) " +
            "values (#{operatorId},#{operatorName},#{operatorRole},#{operationType},#{targetType},#{targetId},#{detail},#{createTime})")
    void insert(OperationLog log);

    @Select("select * from admin_operation_log order by create_time desc limit 200")
    List<OperationLog> listRecent();

    @Select("select * from admin_operation_log where operator_id = #{operatorId} order by create_time desc limit 50")
    List<OperationLog> listByOperator(Long operatorId);

    @Select("select * from admin_operation_log where operation_type = #{type} order by create_time desc limit 50")
    List<OperationLog> listByType(String type);
}
