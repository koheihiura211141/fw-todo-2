package com.example.todoapp;

import com.example.todoapp.api.TodoApiController;
import java.util.List;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.*;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;
import org.springframework.test.context.web.WebAppConfiguration;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;
import static org.mockito.Mockito.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.*;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringJUnitConfig(TodoOwnershipTests.Config.class)
@WebAppConfiguration
class TodoOwnershipTests {
    @Configuration @EnableWebMvc @EnableWebSecurity
    @Import({SecurityConfig.class, TodoService.class, TodoApiController.class, HomeController.class})
    static class Config {
        @Bean SampleTodoService samples() { return mock(SampleTodoService.class); }
        @Bean TodoMapper mapper() { return mock(TodoMapper.class); }
    }
    @Autowired WebApplicationContext context;
    @Autowired TodoMapper mapper;
    @Autowired TodoService service;
    MockMvc mvc;
    LoginUser alice;

    @BeforeEach void setup() {
        reset(mapper);
        AppUser account = new AppUser();
        account.setId(1L); account.setUsername("alice"); account.setPassword("unused"); account.setRole("USER");
        alice = new LoginUser(account);
        Todo own = new Todo(); own.setId(10L); own.setUserId(1L); own.setTitle("Own");
        Todo other = new Todo(); other.setId(20L); other.setUserId(2L); other.setTitle("Private");
        when(mapper.findById(10L)).thenReturn(own);
        when(mapper.findById(20L)).thenReturn(other);
        mvc = MockMvcBuilders.webAppContextSetup(context).apply(springSecurity()).build();
    }
    @AfterEach void clear() { SecurityContextHolder.clearContext(); }
    void login() {
        SecurityContextHolder.getContext().setAuthentication(
                UsernamePasswordAuthenticationToken.authenticated(alice, null, alice.getAuthorities()));
    }
    @Test void ownTodoIsVisibleAndOtherTodoIsForbidden() throws Exception {
        mvc.perform(get("/api/todos/10").with(user(alice))).andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("Own"));
        for (String path : List.of("/api/todos/20", "/todos/20/edit", "/todos/20/delete")) {
            mvc.perform(get(path).with(user(alice))).andExpect(status().isForbidden());
        }
    }
    @Test void foreignMutationsAndConfirmationPagesAreForbidden() throws Exception {
        mvc.perform(put("/api/todos/20").with(user(alice)).with(csrf())
                .contentType("application/json")
                .content("{\"title\":\"Changed\",\"category\":\"Test\",\"priority\":2}"))
                .andExpect(status().isForbidden());
        mvc.perform(delete("/api/todos/20").with(user(alice)).with(csrf())).andExpect(status().isForbidden());
        mvc.perform(patch("/api/todos/20/completed").with(user(alice)).with(csrf())
                .contentType("application/json").content("{\"completed\":true}"))
                .andExpect(status().isForbidden());
        for (String path : List.of("/todos/20", "/todos/20/confirm", "/todos/20/edit", "/todos/20/delete")) {
            mvc.perform(post(path).with(user(alice)).with(csrf())).andExpect(status().isForbidden());
        }
        verify(mapper, never()).update(any());
        verify(mapper, never()).updateCompleted(any(), anyBoolean(), any());
        verify(mapper, never()).deleteById(any(), any());
    }
    @Test void mixedBulkDeleteDoesNotDeleteAnything() throws Exception {
        mvc.perform(post("/todos/bulk-delete").with(user(alice)).with(csrf()).param("ids", "10", "20"))
                .andExpect(status().isForbidden());
        verify(mapper, never()).deleteByIds(any(), any());
    }
    @Test void creationOverridesForgedOwnerAndUpdatesKeepOwner() {
        login();
        Todo todo = new Todo(); todo.setUserId(2L);
        service.create(todo);
        assertEquals(1L, todo.getUserId());
        verify(mapper).insert(todo);
        todo.setId(10L); todo.setUserId(2L);
        service.update(todo);
        assertEquals(1L, todo.getUserId());
        verify(mapper).update(todo);
    }
    @Test void listingAndCountAreScopedToAuthenticatedId() {
        login();
        when(mapper.searchPage(any(), any(), any(), any(), any(), any(), any(), any(), anyInt(), anyLong(), eq(1L)))
                .thenReturn(List.of());
        service.search("", "", "asc", org.springframework.data.domain.PageRequest.of(0, 10));
        verify(mapper).searchPage("", "", "asc", "deadline", null, null, null, null, 10, 0L, 1L);
        verify(mapper).countSearch("", "", null, null, null, null, 1L);
    }
    @Test void kanbanStatusUpdatesRequireCsrfAndOwnership() throws Exception {
        mvc.perform(patch("/api/todos/10/status").with(user(alice))
                .contentType("application/json").content("{\"status\":\"IN_PROGRESS\"}"))
                .andExpect(status().isForbidden());
        mvc.perform(patch("/api/todos/20/status").with(user(alice)).with(csrf())
                .contentType("application/json").content("{\"status\":\"DONE\"}"))
                .andExpect(status().isForbidden());
        verify(mapper, never()).updateStatus(any(), any(), any());
        when(mapper.updateStatus(10L, TodoStatus.IN_PROGRESS, 1L)).thenAnswer(invocation -> {
            mapper.findById(10L).setStatus(TodoStatus.IN_PROGRESS);
            return 1;
        });
        mvc.perform(patch("/api/todos/10/status").with(user(alice)).with(csrf())
                .contentType("application/json").content("{\"status\":\"IN_PROGRESS\"}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.status").value("IN_PROGRESS"));
        verify(mapper).updateStatus(10L, TodoStatus.IN_PROGRESS, 1L);
    }
    @Test void kanbanStatusRejectsInvalidAndMissingTasks() throws Exception {
        for (String body : List.of("{}", "{\"status\":null}", "{\"status\":\"UNKNOWN\"}")) {
            mvc.perform(patch("/api/todos/10/status").with(user(alice)).with(csrf())
                    .contentType("application/json").content(body)).andExpect(status().isBadRequest());
        }
        mvc.perform(patch("/api/todos/99/status").with(user(alice)).with(csrf())
                .contentType("application/json").content("{\"status\":\"DONE\"}"))
                .andExpect(status().isNotFound());
        verify(mapper, never()).updateStatus(any(), any(), any());
    }
    @Test void statisticsAreScopedToAuthenticatedUser() throws Exception {
        when(mapper.countByCategory(1L)).thenReturn(List.of(new TaskCount("仕事", 4)));
        when(mapper.countByPriority(1L)).thenReturn(List.of(
                new TaskCount("HIGH", 1), new TaskCount("MEDIUM", 3), new TaskCount("LOW", 0)));
        mvc.perform(get("/api/todos/stats/categories").with(user(alice)))
                .andExpect(status().isOk()).andExpect(jsonPath("$[0].label").value("仕事"))
                .andExpect(jsonPath("$[0].count").value(4));
        mvc.perform(get("/api/todos/stats/priorities").with(user(alice)))
                .andExpect(status().isOk()).andExpect(jsonPath("$[2].label").value("LOW"))
                .andExpect(jsonPath("$[2].count").value(0));
        verify(mapper).countByCategory(1L);
        verify(mapper).countByPriority(1L);
    }
    @Test void statisticsRequireLogin() throws Exception {
        mvc.perform(get("/api/todos/stats/categories")).andExpect(status().isUnauthorized());
        mvc.perform(get("/api/todos/stats/priorities")).andExpect(status().isUnauthorized());
        verify(mapper, never()).countByCategory(any());
        verify(mapper, never()).countByPriority(any());
    }
    @Test void emptyCategoryStatisticsReturnEmptyArray() throws Exception {
        when(mapper.countByCategory(1L)).thenReturn(List.of());
        mvc.perform(get("/api/todos/stats/categories").with(user(alice)))
                .andExpect(status().isOk()).andExpect(content().json("[]"));
    }
    @Test void legacyCompletionTakesPrecedenceOverStoredStatus() {
        Todo todo = new Todo();
        assertEquals(TodoStatus.NOT_STARTED, todo.getStatus());
        todo.setStatus(TodoStatus.IN_PROGRESS);
        assertEquals(TodoStatus.IN_PROGRESS, todo.getStatus());
        todo.setCompleted(true);
        assertEquals(TodoStatus.DONE, todo.getStatus());
    }
    @Test void serviceRejectsMissingIdentityIncludingMcpCalls() {
        assertThrows(org.springframework.security.access.AccessDeniedException.class, () -> service.findById(10L));
        assertThrows(org.springframework.security.access.AccessDeniedException.class, () -> service.create(new Todo()));
        verify(mapper, never()).insert(any());
    }
}
