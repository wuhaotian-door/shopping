package com.class242014group06.aspect;


import com.class242014group06.annotation.AutoFill;

import com.class242014group06.constant.AutoFillConstant;
import com.class242014group06.context.BaseContext;
import com.class242014group06.enumeration.OperationType;




import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.aspectj.lang.annotation.Pointcut;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.stereotype.Component;

import java.lang.reflect.Method;
import java.time.LocalDateTime;

@Aspect
@Component
@Slf4j
public class AutoFillAspect {
    /**
     * 切入点
     */
    @Pointcut("execution(* com.class242014group06.mapper.*.*(..)) && @annotation(com.class242014group06.annotation.AutoFill)")
    public void autoFillPointCut() {
    }

    /**
     * 前置通知,在通知中进行公共字段的赋值
     */
    @Before("autoFillPointCut()")
    public void autoFill(JoinPoint joinPoint) {
        log.info("开始进行公共字段自动填充...");

        //获取被拦截方法的数据库操作类型
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();//方法签名对象
        AutoFill autoFill = signature.getMethod().getAnnotation(AutoFill.class);//获得方法上的注解对象
        OperationType operationType = autoFill.value();//通过注解获方法的数据库操作类型

        //获取被拦截方法的参数--实体对象,这里约定方法的第一个参数就是实体对象
        Object[] args = joinPoint.getArgs();
        if(args == null || args.length == 0){
            return;
        }

        Object entity = args[0];

        //准备赋值的数据
        LocalDateTime now = LocalDateTime.now();
        Long currentId = BaseContext.getCurrentId();
        // 当用户自行注册等无需登录的场景下,currentId 为 null,使用 0L 占位
        if (currentId == null) {
            currentId = 0L;
        }

        //根据不同的操作类型,为对应的属性通过反射来赋值
        if(operationType == OperationType.INSERT){
            //为四个公共字段赋值（逐个 try-catch，避免某个字段不存在导致其他字段也无法赋值）
            try {
                Method setCreateTime = entity.getClass().getDeclaredMethod(AutoFillConstant.SET_CREATE_TIME, LocalDateTime.class);
                setCreateTime.invoke(entity, now);
            } catch (Exception e) {
                // 实体可能没有 createTime 字段，忽略
            }
            try {
                Method setCreateUser = entity.getClass().getDeclaredMethod(AutoFillConstant.SET_CREATE_USER, Long.class);
                setCreateUser.invoke(entity, currentId);
            } catch (Exception e) {
                // 实体可能没有 createUser 字段（如 AppointmentSlot、InheritorProfile），忽略
            }
            try {
                Method setUpdateTime = entity.getClass().getDeclaredMethod(AutoFillConstant.SET_UPDATE_TIME, LocalDateTime.class);
                setUpdateTime.invoke(entity, now);
            } catch (Exception e) {
                // 实体可能没有 updateTime 字段，忽略
            }
            try {
                Method setUpdateUser = entity.getClass().getDeclaredMethod(AutoFillConstant.SET_UPDATE_USER, Long.class);
                setUpdateUser.invoke(entity, currentId);
            } catch (Exception e) {
                // 实体可能没有 updateUser 字段，忽略
            }
        }else if(operationType == OperationType.UPDATE){
            // UPDATE 只填充 updateTime 和 updateUser，不覆盖 createTime 和 createUser
            try {
                Method setUpdateTime = entity.getClass().getDeclaredMethod(AutoFillConstant.SET_UPDATE_TIME, LocalDateTime.class);
                setUpdateTime.invoke(entity, now);
            } catch (Exception e) {
                // 实体可能没有 updateTime 字段，忽略
            }
            try {
                Method setUpdateUser = entity.getClass().getDeclaredMethod(AutoFillConstant.SET_UPDATE_USER, Long.class);
                setUpdateUser.invoke(entity, currentId);
            } catch (Exception e) {
                // 实体可能没有 updateUser 字段，忽略
            }
        }
    }
}
