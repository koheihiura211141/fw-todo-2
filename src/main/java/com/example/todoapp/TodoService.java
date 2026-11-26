package com.example.todoapp;

import java.util.List;
import java.time.LocalDate;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class TodoService {

    private final TodoMapper todoMapper;

    public TodoService(TodoMapper todoMapper) {
        this.todoMapper = todoMapper;
    }

    public Page<Todo> search(String keyword, String category, String order, Pageable pageable) {
        return search(keyword, category, order, null, null, pageable);
    }

    public Page<Todo> search(String keyword, String category, String order,
                             LocalDate from, LocalDate to, Pageable pageable) {
        return search(keyword, category, order, null, null, from, to, pageable);
    }

    public Page<Todo> search(String keyword, String category, String order,
                             Integer priority, Boolean completed,
                             LocalDate from, LocalDate to, Pageable pageable) {
        List<Todo> todos = todoMapper.searchPage(
                keyword, category, order, pageable.getSort().stream().findFirst().map(org.springframework.data.domain.Sort.Order::getProperty).orElse("deadline"), priority, completed, from, to, pageable.getPageSize(), pageable.getOffset());
        long total = todoMapper.countSearch(keyword, category, priority, completed, from, to);
        return new PageImpl<>(todos, pageable, total);
    }

    public Todo findById(Long id) {
        return todoMapper.findById(id);
    }

    public int create(Todo todo) {
        int result = todoMapper.insert(todo);
        log.info("登録: id={}", todo.getId());
        return result;
    }

    public int update(Todo todo) {
        int result = todoMapper.update(todo);
        log.info("編集: id={}", todo.getId());
        return result;
    }

    public int updateCompleted(Long id, boolean completed) {
        return todoMapper.updateCompleted(id, completed);
    }

    public int deleteAll(List<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            return 0;
        }
        int result = todoMapper.deleteByIds(ids);
        log.info("一括削除: ids={}, count={}", ids, result);
        return result;
    }

    public int delete(Long id) {
        int result = todoMapper.deleteById(id);
        log.info("削除: id={}", id);
        return result;
    }
}
