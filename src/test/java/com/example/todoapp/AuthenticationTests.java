package com.example.todoapp;

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

import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.*;
import static org.springframework.security.test.web.servlet.response.SecurityMockMvcResultMatchers.*;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringJUnitConfig(AuthenticationTests.Config.class)
@WebAppConfiguration
class AuthenticationTests {
    @Configuration
    @EnableWebMvc
    @EnableWebSecurity
    @org.springframework.data.web.config.EnableSpringDataWebSupport
    @Import({SecurityConfig.class, AuthController.class, UserAccountService.class, HomeController.class})
    static class Config {
        @Bean SampleTodoService samples() { return mock(SampleTodoService.class); }
        @Bean UserMapper users() { return mock(UserMapper.class); }
        @Bean TodoService todos() {
            var service = mock(TodoService.class);
            when(service.search(anyString(), anyString(), anyString(), any(), any(), any(), any(), any()))
                    .thenReturn(org.springframework.data.domain.Page.empty());
            return service;
        }
        @Bean org.thymeleaf.spring6.view.ThymeleafViewResolver viewResolver(WebApplicationContext context) {
            var templates = new org.thymeleaf.spring6.templateresolver.SpringResourceTemplateResolver();
            templates.setApplicationContext(context);
            templates.setPrefix("classpath:/templates/");
            templates.setSuffix(".html");
            templates.setCharacterEncoding("UTF-8");
            var engine = new org.thymeleaf.spring6.SpringTemplateEngine();
            engine.setTemplateResolver(templates);
            engine.addDialect(new org.thymeleaf.extras.springsecurity6.dialect.SpringSecurityDialect());
            var resolver = new org.thymeleaf.spring6.view.ThymeleafViewResolver();
            resolver.setTemplateEngine(engine);
            resolver.setCharacterEncoding("UTF-8");
            return resolver;
        }
    }
    @Autowired WebApplicationContext context;
    @Autowired UserMapper users;
    @Autowired SampleTodoService samples;
    @Autowired org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder encoder;
    MockMvc mvc;

    @BeforeEach void setup() {
        reset(users, samples);
        mvc = MockMvcBuilders.webAppContextSetup(context).apply(springSecurity()).build();
    }

    @Test void protectsPagesButAllowsLoginAndResources() throws Exception {
        mvc.perform(get("/todos")).andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/login"));
        mvc.perform(get("/api/todos")).andExpect(status().isUnauthorized());
        mvc.perform(get("/login")).andExpect(status().isOk()).andExpect(view().name("login"))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("name=\"_csrf\"")));
        mvc.perform(get("/login?error")).andExpect(content().string(
                org.hamcrest.Matchers.containsString("ユーザー名またはパスワードが正しくありません。")));
        for (String path : new String[]{"/css/test.css", "/js/test.js", "/images/test.png"}) {
            mvc.perform(get(path)).andExpect(status().isNotFound());
        }
    }

    @Test void authenticatesDatabaseUserAndRejectsBadPassword() throws Exception {
        AppUser user = new AppUser();
        user.setId(1L); user.setUsername("alice"); user.setPassword(encoder.encode("password123")); user.setRole("USER");
        when(users.findByUsername("alice")).thenReturn(user);
        mvc.perform(post("/login").with(csrf()).param("username", "alice").param("password", "password123"))
                .andExpect(redirectedUrl("/todos")).andExpect(authenticated().withUsername("alice"));
        mvc.perform(post("/login").with(csrf()).param("username", "alice").param("password", "wrong"))
                .andExpect(redirectedUrl("/login?error")).andExpect(unauthenticated());
        verify(samples, times(1)).initialize(1L);
    }

    @Test void authenticatedUserCanRenderListAndKanban() throws Exception {
        AppUser account = new AppUser();
        account.setId(1L); account.setUsername("alice"); account.setPassword("unused"); account.setRole("USER");
        LoginUser principal = new LoginUser(account);
        Todo todo = new Todo();
        todo.setId(10L); todo.setUserId(1L); todo.setTitle("Board regression");
        todo.setCategory("Test"); todo.setPriority(2); todo.setCompleted(false);
        todo.setStatus(TodoStatus.IN_PROGRESS);
        TodoService service = context.getBean(TodoService.class);
        when(service.search(anyString(), anyString(), anyString(), any(), any(), any(), any(), any()))
                .thenReturn(new org.springframework.data.domain.PageImpl<>(java.util.List.of(todo)));
        when(service.search(anyString(), anyString(), anyString(), any(org.springframework.data.domain.Pageable.class)))
                .thenReturn(new org.springframework.data.domain.PageImpl<>(java.util.List.of(todo)));
        mvc.perform(get("/todos").with(user(principal)))
                .andExpect(status().isOk()).andExpect(content().string(org.hamcrest.Matchers.containsString("Board regression")));
        mvc.perform(get("/todos/kanban").with(user(principal)))
                .andExpect(status().isOk()).andExpect(content().string(org.hamcrest.Matchers.containsString("Board regression")));
    }

    @Test void guestLoginIsUnavailableAndOldGuestSessionsAreDenied() throws Exception {
        mvc.perform(post("/guest").with(csrf())).andExpect(status().is3xxRedirection());
        mvc.perform(get("/todos").with(user("guest").roles("GUEST")))
                .andExpect(status().isForbidden());
        mvc.perform(get("/login")).andExpect(content().string(
                org.hamcrest.Matchers.not(org.hamcrest.Matchers.containsString("/guest"))));
    }
    @Test void registrationHashesPasswordAndFixesRole() throws Exception {
        mvc.perform(post("/register").with(csrf()).param("username", "alice")
                        .param("password", "password123").param("role", "ADMIN"))
                .andExpect(redirectedUrl("/login?registered"));
        var captor = org.mockito.ArgumentCaptor.forClass(AppUser.class);
        verify(users).insert(captor.capture());
        org.junit.jupiter.api.Assertions.assertTrue(encoder.matches("password123", captor.getValue().getPassword()));
        org.junit.jupiter.api.Assertions.assertEquals("USER", captor.getValue().getRole());
    }

    @Test void rejectsInvalidAndDuplicateRegistrations() throws Exception {
        mvc.perform(post("/register").with(csrf()))
                .andExpect(view().name("register")).andExpect(model().attributeHasFieldErrors("registration", "username", "password"));
        mvc.perform(post("/register").with(csrf()).param("username", "a").param("password", "short"))
                .andExpect(view().name("register")).andExpect(model().attributeHasFieldErrors("registration", "username", "password"));
        verifyNoInteractions(users);
        when(users.insert(any())).thenThrow(new org.springframework.dao.DuplicateKeyException("duplicate"));
        mvc.perform(post("/register").with(csrf()).param("username", "alice").param("password", "password123"))
                .andExpect(view().name("register")).andExpect(model().attributeHasFieldErrors("registration", "username"));
    }
}
