package com.sky.aspect;

import com.sky.annotation.AutoFill;
import com.sky.constant.AutoFillConstant;
import com.sky.context.BaseContext;
import com.sky.enumeration.OperationType;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.Signature;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.aspectj.lang.annotation.Pointcut;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

/**
 * 自动填充切面类，用于在方法执行前自动填充实体类的创建时间和更新时间字段
 */
@Aspect
@Component
@Slf4j
public class AutoFillAspect {
    /**
     * 切入点
     * 对所有添加了@AutoFill注解的方法进行拦截
     */
    @Pointcut("execution(* com.sky.mapper.*.*(..)) && @annotation(com.sky.annotation.AutoFill)")    // 切点表达式
    public void autoFillPointCut() {}

    /**
     * 前置通知，在方法执行前自动填充实体类的创建时间和更新时间字段
     * @param joinPoint 连接点
     */
    @Before("autoFillPointCut()")
    public void autoFill(JoinPoint joinPoint){
        log.info("自动填充创建时间和更新时间字段");

        // 获取到当前被拦截的方法上的数据库操作类型
        MethodSignature signature = (MethodSignature) joinPoint.getSignature(); // 方法签名对象
        AutoFill autoFill = signature.getMethod().getAnnotation(AutoFill.class); // 获取到方法上的@AutoFill注解对象
        OperationType operationType = autoFill.value(); // 获取到数据库操作类型

        // 获取到方法上的参数--实体对象
        Object[] args = joinPoint.getArgs(); // 获取到方法上的参数--实体对象数组
        if(args == null || args.length == 0){
            return;
        }

        Object entity = args[0]; // 获取到方法上的参数--实体对象

        // 准备赋值数据
        LocalDateTime now = LocalDateTime.now(); // 获取当前时间
        Long currentId = BaseContext.getCurrentId(); // 获取当前登录用户的id


        // 根据当前不同的操作类型，通过反射为对应属性赋值
        if (operationType == OperationType.INSERT){
            // 为4个公共字段赋值
            try {
                entity.getClass().getDeclaredMethod(AutoFillConstant.SET_CREATE_TIME, LocalDateTime.class).invoke(entity, now);
                entity.getClass().getDeclaredMethod(AutoFillConstant.SET_UPDATE_TIME, LocalDateTime.class).invoke(entity, now);
                entity.getClass().getDeclaredMethod(AutoFillConstant.SET_CREATE_USER, Long.class).invoke(entity, currentId);
                entity.getClass().getDeclaredMethod(AutoFillConstant.SET_UPDATE_USER, Long.class).invoke(entity, currentId);
            } catch (Exception e) {
                log.error("自动填充创建时间、更新时间、创建用户、更新用户字段失败", e);
            }
        } else if (operationType == OperationType.UPDATE){
            // 为2个公共字段赋值
            try {
                entity.getClass().getDeclaredMethod(AutoFillConstant.SET_UPDATE_TIME, LocalDateTime.class).invoke(entity, now);
                entity.getClass().getDeclaredMethod(AutoFillConstant.SET_UPDATE_USER, Long.class).invoke(entity, currentId);
            } catch (Exception e) {
                log.error("自动填充更新时间和更新用户字段失败", e);
            }
        }
    }
}
