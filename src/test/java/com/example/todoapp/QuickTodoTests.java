package com.example.todoapp;

import com.example.todoapp.api.TodoApiController;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;
import org.springframework.test.context.web.WebAppConfiguration;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringJUnitConfig(QuickTodoTests.Config.class)
@WebAppConfiguration
@org.springframework.security.test.context.support.WithMockUser
class QuickTodoTests {
    @Configuration
    @EnableWebMvc
    @EnableWebSecurity
    @Import({SecurityConfig.class, TodoApiController.class})
    static class Config {
        @Bean SampleTodoService samples() { return mock(SampleTodoService.class); }
        @Bean TodoService todoService() { return mock(TodoService.class); }
    }

    @Autowired WebApplicationContext context;
    @Autowired TodoService service;
    MockMvc mvc;

    @BeforeEach
    void setup() {
        reset(service);
        mvc = MockMvcBuilders.webAppContextSetup(context).apply(springSecurity()).build();
    }

    @Test
    void updatesOnlyCompletionInBothDirections() throws Exception {
        Todo saved = new Todo();
        saved.setId(7L);
        saved.setTitle("買い物");
        when(service.findById(7L)).thenReturn(saved);
        when(service.updateCompleted(org.mockito.ArgumentMatchers.eq(7L), anyBoolean()))
                .thenAnswer(invocation -> {
                    saved.setCompleted(invocation.getArgument(1));
                    return 1;
                });
        for (boolean completed : new boolean[]{true, false}) {
            mvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders
                    .patch("/api/todos/7/completed").with(csrf().asHeader())
                    .contentType("application/json").content("{\"completed\":" + completed + "}"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.completed").value(completed))
                    .andExpect(jsonPath("$.title").value("買い物"));
            verify(service).updateCompleted(7L, completed);
        }
        verify(service, never()).update(any());
    }

    @Test
    void completionRejectsInvalidRequests() throws Exception {
        mvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders
                .patch("/api/todos/7/completed").with(csrf().asHeader())
                .contentType("application/json").content("{}"))
                .andExpect(status().isBadRequest());
        mvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders
                .patch("/api/todos/7/completed")
                .contentType("application/json").content("{\"completed\":true}"))
                .andExpect(status().isForbidden());
        verifyNoInteractions(service);
        mvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders
                .patch("/api/todos/999/completed").with(csrf().asHeader())
                .contentType("application/json").content("{\"completed\":true}"))
                .andExpect(status().isNotFound());
        verify(service, never()).updateCompleted(any(), anyBoolean());
    }

    @Test
    void createsTitleOnlyTaskWithDefaults() throws Exception {
        Todo saved = new Todo();
        saved.setId(7L);
        saved.setTitle("買い物");
        saved.setCategory("その他");
        saved.setPriority(2);
        saved.setCompleted(false);
        when(service.create(any())).thenAnswer(invocation -> {
            Todo todo = invocation.getArgument(0);
            org.junit.jupiter.api.Assertions.assertEquals("買い物", todo.getTitle());
            org.junit.jupiter.api.Assertions.assertEquals("その他", todo.getCategory());
            org.junit.jupiter.api.Assertions.assertEquals(2, todo.getPriority());
            org.junit.jupiter.api.Assertions.assertEquals(false, todo.getCompleted());
            todo.setId(7L);
            return 1;
        });
        when(service.findById(7L)).thenReturn(saved);
        mvc.perform(post("/api/todo").with(csrf().asHeader()).contentType("application/json")
                .content("{\"title\":\" 買い物 \"}"))
                .andExpect(status().isCreated()).andExpect(header().string("Location", "/api/todos/7"))
                .andExpect(jsonPath("$.title").value("買い物"));
    }

    @Test
    void invalidTitlesReturnInlineErrorPayloadWithoutSaving() throws Exception {
        for (String title : new String[]{"", "   ", "a".repeat(256)}) {
            mvc.perform(post("/api/todo").with(csrf().asHeader()).contentType("application/json")
                    .content("{\"title\":\"" + title + "\"}"))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.errors[0].field").value("title"));
        }
        verifyNoInteractions(service);
    }

    @Test
    void missingOrInvalidCsrfTokenIsRejected() throws Exception {
        mvc.perform(post("/api/todo").contentType("application/json").content("{\"title\":\"test\"}"))
                .andExpect(status().isForbidden());
        mvc.perform(post("/api/todo").with(csrf().useInvalidToken()).contentType("application/json")
                .content("{\"title\":\"test\"}"))
                .andExpect(status().isForbidden());
        verifyNoInteractions(service);
    }
}
