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
    private Long currentUserId() {
        var authentication = org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()
                || !(authentication.getPrincipal() instanceof LoginUser user) || user.getId() == null) {
            throw new org.springframework.security.access.AccessDeniedException("A registered user is required");
        }
        return user.getId();
    }

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
        Long userId = currentUserId();
        List<Todo> todos = todoMapper.searchPage(
                keyword, category, order, pageable.getSort().stream().findFirst().map(org.springframework.data.domain.Sort.Order::getProperty).orElse("deadline"), priority, completed, from, to, pageable.getPageSize(), pageable.getOffset(), userId);
        long total = todoMapper.countSearch(keyword, category, priority, completed, from, to, userId);
        return new PageImpl<>(todos, pageable, total);
    }

    public Todo findById(Long id) {
        Long userId = currentUserId();
        Todo todo = todoMapper.findById(id);
        if (todo != null && !userId.equals(todo.getUserId())) {
            throw new org.springframework.security.access.AccessDeniedException("This Todo belongs to another user");
        }
        return todo;
    }

    public int create(Todo todo) {
        todo.setUserId(currentUserId());
        int result = todoMapper.insert(todo);
        log.info("登録: id={}", todo.getId());
        return result;
    }

    public int update(Todo todo) {
        findById(todo.getId());
        todo.setUserId(currentUserId());
        int result = todoMapper.update(todo);
        log.info("編集: id={}", todo.getId());
        return result;
    }

    public int updateCompleted(Long id, boolean completed) {
        findById(id);
        return todoMapper.updateCompleted(id, completed, currentUserId());
    }

    @org.springframework.transaction.annotation.Transactional
    public int deleteAll(List<Long> ids) {
        currentUserId();
        if (ids == null || ids.isEmpty()) {
            return 0;
        }
        ids.forEach(this::findById);
        int result = todoMapper.deleteByIds(ids, currentUserId());
        log.info("一括削除: ids={}, count={}", ids, result);
        return result;
    }

    public int delete(Long id) {
        findById(id);
        int result = todoMapper.deleteById(id, currentUserId());
        log.info("削除: id={}", id);
        return result;
    }
}
