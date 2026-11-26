package com.example.todoapp;

import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import jakarta.validation.constraints.NotBlank;

public class RegistrationForm {
    @NotBlank(message = "ユーザー名を入力してください。")
    @Pattern(regexp = "[a-zA-Z0-9_]{3,50}", message = "ユーザー名は3〜50文字の半角英数字・_で入力してください。")
    private String username;
    @NotBlank(message = "パスワードを入力してください。")
    @Size(min = 8, max = 72, message = "パスワードは8〜72文字で入力してください。")
    private String password;

    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }
    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }
}
