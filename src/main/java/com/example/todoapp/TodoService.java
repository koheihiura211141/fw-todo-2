package com.example.todoapp;

import java.util.List;

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
        List<Todo> todos = todoMapper.searchPage(
                keyword, category, order, pageable.getPageSize(), pageable.getOffset());
        long total = todoMapper.countSearch(keyword, category);
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

    public int delete(Long id) {
        int result = todoMapper.deleteById(id);
        log.info("削除: id={}", id);
        return result;
    }
}
