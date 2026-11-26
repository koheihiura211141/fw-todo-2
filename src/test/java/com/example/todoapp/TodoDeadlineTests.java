package com.example.todoapp;

import java.time.LocalDate;
import java.util.List;
import java.util.Locale;

import org.junit.jupiter.api.Test;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.web.PageableHandlerMethodArgumentResolver;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.thymeleaf.spring6.SpringTemplateEngine;
import org.thymeleaf.spring6.view.ThymeleafViewResolver;
import org.thymeleaf.templateresolver.ClassLoaderTemplateResolver;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;

class TodoDeadlineTests {
    @Test
    void rendersDeadlineWarningsAtBoundariesAndHandlesMissingDates() throws Exception {
        LocalDate today = LocalDate.now();
        List<Todo> todos = java.util.stream.Stream.of(
                today.minusDays(1), today, today.plusDays(3), today.plusDays(4), null)
                .map(date -> {
                    Todo todo = new Todo();
                    todo.setId(1L);
                    todo.setTitle("Task");
                    todo.setCategory("Test");
                    todo.setPriority(1);
                    todo.setCompleted(false);
                    todo.setDeadline(date);
                    return todo;
                }).toList();
        TodoService service = mock(TodoService.class);
        when(service.search(anyString(), anyString(), anyString(), isNull(), isNull(),
                isNull(), isNull(), any())).thenReturn(new PageImpl<>(todos, PageRequest.of(0, 10), 5));
        var resolver = new ClassLoaderTemplateResolver();
        resolver.setPrefix("templates/");
        resolver.setSuffix(".html");
        resolver.setCharacterEncoding("UTF-8");
        var engine = new SpringTemplateEngine();
        engine.setTemplateResolver(resolver);
        var views = new ThymeleafViewResolver();
        views.setTemplateEngine(engine);
        views.setCharacterEncoding("UTF-8");
        var mvc = MockMvcBuilders.standaloneSetup(new HomeController(service))
                .setCustomArgumentResolvers(new PageableHandlerMethodArgumentResolver())
                .setViewResolvers(views).build();
        var response = mvc.perform(get("/todos").locale(Locale.JAPAN)).andReturn().getResponse();
        assertEquals(200, response.getStatus());
        String html = response.getContentAsString();
        assertEquals(1, html.split("class=\"deadline-overdue\"", -1).length - 1);
        assertEquals(2, html.split("class=\"deadline-upcoming\"", -1).length - 1);
        assertTrue(html.contains(today.toString().replace('-', '/')));
        assertTrue(html.contains("期限切れ"));
        assertTrue(html.contains("（入力なし）"));
    }

    @Test
    void deadlineSurvivesFormConversionAndLegacyAlias() {
        LocalDate date = LocalDate.of(2026, 12, 1);
        TodoForm form = new TodoForm();
        form.setDeadline(date);
        Todo todo = form.toTodo();
        assertEquals(date, todo.getDeadline());
        assertEquals(date, todo.getDueDate());
        assertEquals(date, TodoForm.fromTodo(todo).getDeadline());
        todo.setDueDate(null);
        assertNull(todo.getDeadline());
    }
}