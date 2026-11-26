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
                          @Param("sortBy") String sortBy,
                          @Param("priority") Integer priority,
                          @Param("completed") Boolean completed,
                          @Param("from") LocalDate from,
                          @Param("to") LocalDate to,
                          @Param("limit") int limit,
                          @Param("offset") long offset, @Param("userId") Long userId);

    long countSearch(@Param("keyword") String keyword, @Param("category") String category,
                     @Param("priority") Integer priority, @Param("completed") Boolean completed,
                     @Param("from") LocalDate from, @Param("to") LocalDate to, @Param("userId") Long userId);

    List<TaskCount> countByCategory(@Param("userId") Long userId);
    List<TaskCount> countByPriority(@Param("userId") Long userId);

    Todo findById(Long id);

    int insert(Todo todo);

    int update(Todo todo);

    int updateCompleted(@Param("id") Long id, @Param("completed") boolean completed, @Param("userId") Long userId);

    int updateStatus(@Param("id") Long id, @Param("status") TodoStatus status, @Param("userId") Long userId);

    int deleteById(@Param("id") Long id, @Param("userId") Long userId);

    int deleteByIds(@Param("ids") List<Long> ids, @Param("userId") Long userId);
}
