package com.example.todoapp;

import java.nio.charset.StandardCharsets;
import java.util.List;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.security.web.authentication.session.ChangeSessionIdAuthenticationStrategy;
import org.springframework.security.web.csrf.CsrfAuthenticationStrategy;
import org.springframework.security.web.csrf.HttpSessionCsrfTokenRepository;
import org.springframework.stereotype.Controller;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;

@Controller
public class AuthController {
    private final UserAccountService accounts;

    public AuthController(UserAccountService accounts) { this.accounts = accounts; }

    @GetMapping("/login")
    public String login() { return "login"; }

    @GetMapping("/register")
    public String register(@ModelAttribute("registration") RegistrationForm form) { return "register"; }

    @PostMapping("/register")
    public String register(@Valid @ModelAttribute("registration") RegistrationForm form, BindingResult errors) {
        if (form.getPassword() != null && form.getPassword().getBytes(StandardCharsets.UTF_8).length > 72) {
            errors.rejectValue("password", "length", "パスワードはUTF-8で72バイト以内にしてください。");
        }
        if (errors.hasErrors()) return "register";
        try {
            accounts.register(form.getUsername(), form.getPassword());
        } catch (DuplicateKeyException ex) {
            errors.rejectValue("username", "duplicate", "このユーザー名は既に使用されています。");
            return "register";
        }
        return "redirect:/login?registered";
    }

    @PostMapping("/guest")
    public String guest(HttpServletRequest request, HttpServletResponse response) {
        var current = SecurityContextHolder.getContext().getAuthentication();
        if (current != null && current.isAuthenticated()
                && !(current instanceof org.springframework.security.authentication.AnonymousAuthenticationToken)) {
            return "redirect:/todos";
        }
        var authentication = UsernamePasswordAuthenticationToken.authenticated("ゲスト", null,
                List.of(new SimpleGrantedAuthority("ROLE_GUEST")));
        new ChangeSessionIdAuthenticationStrategy().onAuthentication(authentication, request, response);
        new CsrfAuthenticationStrategy(new HttpSessionCsrfTokenRepository())
                .onAuthentication(authentication, request, response);
        var context = SecurityContextHolder.createEmptyContext();
        context.setAuthentication(authentication);
        SecurityContextHolder.setContext(context);
        new HttpSessionSecurityContextRepository().saveContext(context, request, response);
        return "redirect:/todos";
    }
}
