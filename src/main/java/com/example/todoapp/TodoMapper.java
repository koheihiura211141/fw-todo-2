package com.example.todoapp;

import java.util.List;
import java.time.LocalDate;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface TodoMapper {
    List<Todo> searchPage(@Param("keyword") String keyword,
                          @Param("category") String category,
                          @Param("order") String order,
                          @Param("priority") Integer priority,
                          @Param("completed") Boolean completed,
                          @Param("from") LocalDate from,
                          @Param("to") LocalDate to,
                          @Param("limit") int limit,
                          @Param("offset") long offset);

    long countSearch(@Param("keyword") String keyword, @Param("category") String category,
                     @Param("priority") Integer priority, @Param("completed") Boolean completed,
                     @Param("from") LocalDate from, @Param("to") LocalDate to);

    Todo findById(Long id);

    int insert(Todo todo);

    int update(Todo todo);

    int deleteById(Long id);
}
