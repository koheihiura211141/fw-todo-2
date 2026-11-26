package com.example.todoapp;
import java.util.List;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
public class LoginUser extends User {
    private final Long id;
    public LoginUser(AppUser user) {
        super(user.getUsername(), user.getPassword(), List.of(new SimpleGrantedAuthority("ROLE_" + user.getRole())));
        id = user.getId();
    }
    public Long getId() { return id; }
}