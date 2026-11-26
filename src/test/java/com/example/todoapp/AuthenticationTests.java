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
import org.springframework.mock.web.MockHttpSession;

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
    @Autowired org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder encoder;
    MockMvc mvc;

    @BeforeEach void setup() {
        reset(users);
        mvc = MockMvcBuilders.webAppContextSetup(context).apply(springSecurity()).build();
    }

    @Test void protectsPagesButAllowsLoginAndResources() throws Exception {
        mvc.perform(get("/todos")).andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/login"));
        mvc.perform(get("/api/todos")).andExpect(status().is3xxRedirection());
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
        user.setUsername("alice"); user.setPassword(encoder.encode("password123")); user.setRole("USER");
        when(users.findByUsername("alice")).thenReturn(user);
        mvc.perform(post("/login").with(csrf()).param("username", "alice").param("password", "password123"))
                .andExpect(redirectedUrl("/todos")).andExpect(authenticated().withUsername("alice"));
        mvc.perform(post("/login").with(csrf()).param("username", "alice").param("password", "wrong"))
                .andExpect(redirectedUrl("/login?error")).andExpect(unauthenticated());
    }

    @Test void guestNeedsCsrfAndPersistsUntilLogout() throws Exception {
        mvc.perform(post("/guest")).andExpect(status().isForbidden());
        MockHttpSession session = new MockHttpSession();
        String previousId = session.getId();
        var result = mvc.perform(post("/guest").session(session).with(csrf()).with(request -> {
                    request.setRequestedSessionId(previousId);
                    request.setRequestedSessionIdValid(true);
                    return request;
                }))
                .andExpect(redirectedUrl("/todos"))
                .andExpect(authenticated().withRoles("GUEST")).andReturn();
        org.junit.jupiter.api.Assertions.assertNotEquals(previousId, session.getId());
        mvc.perform(get("/login").session((MockHttpSession) result.getRequest().getSession()))
                .andExpect(authenticated().withRoles("GUEST"));
        mvc.perform(get("/todos").session(session))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("ゲスト")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("action=\"/logout\"")));
        mvc.perform(post("/logout").session(session).with(csrf()))
                .andExpect(redirectedUrl("/login?logout")).andExpect(unauthenticated());
        org.junit.jupiter.api.Assertions.assertTrue(session.isInvalid());
        verifyNoInteractions(users);
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
