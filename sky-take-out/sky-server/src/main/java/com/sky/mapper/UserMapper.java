package com.sky.mapper;

import com.sky.entity.User;
import org.apache.ibatis.annotations.Mapper;

import java.util.Map;

@Mapper
public interface UserMapper {
    // 根据openid查询用户信息
    User getByOpenId(String openid);

    // 插入新用户信息
    void insert(User user);

    // 根据用户id查询用户信息
    User getById(Long userId);

    Integer countByMap(Map map);
}
