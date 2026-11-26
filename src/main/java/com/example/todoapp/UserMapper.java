package com.example.todoapp;

import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface UserMapper {
    @Select("SELECT id, username, password, role FROM users WHERE username = #{username}")
    AppUser findByUsername(String username);

    @Insert("INSERT INTO users (username, password, role) VALUES (#{username}, #{password}, #{role})")
    int insert(AppUser user);
}
