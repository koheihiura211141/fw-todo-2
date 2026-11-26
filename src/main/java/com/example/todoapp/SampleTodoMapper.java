package com.example.todoapp;

import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface SampleTodoMapper {
    // Serialize initialization for the same account, including simultaneous logins.
    @Select("SELECT id FROM users WHERE id = #{userId} FOR UPDATE")
    Long lockUser(Long userId);

    @Select("SELECT COUNT(*) FROM sample_todo_initializations WHERE user_id = #{userId}")
    int countInitialization(Long userId);

    @Insert("INSERT INTO sample_todo_initializations (user_id) VALUES (#{userId})")
    int markInitialized(Long userId);
}
