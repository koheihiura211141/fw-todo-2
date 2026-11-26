package com.example.todoapp;


import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class UserAccountService implements UserDetailsService {
    private final UserMapper users;
    private final BCryptPasswordEncoder encoder;

    public UserAccountService(UserMapper users, BCryptPasswordEncoder encoder) {
        this.users = users;
        this.encoder = encoder;
    }

    @Override
    public UserDetails loadUserByUsername(String username) {
        AppUser user = users.findByUsername(username);
        if (user == null) throw new UsernameNotFoundException("User not found");
        return new LoginUser(user);
    }

    public void register(String username, String password) {
        AppUser user = new AppUser();
        user.setUsername(username);
        user.setPassword(encoder.encode(password));
        user.setRole("USER");
        users.insert(user);
    }
}
