package com.example.todoapp;

import java.util.List;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface TodoMapper {
    List<Todo> searchPage(@Param("keyword") String keyword,
                          @Param("category") String category,
                          @Param("order") String order,
                          @Param("limit") int limit,
                          @Param("offset") long offset);

    long countSearch(@Param("keyword") String keyword, @Param("category") String category);

    Todo findById(Long id);

    int insert(Todo todo);

    int update(Todo todo);

    int deleteById(Long id);
}
