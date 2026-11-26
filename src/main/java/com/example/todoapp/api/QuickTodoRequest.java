package com.example.todoapp.api;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record QuickTodoRequest(
        @NotBlank(message = "タイトルを入力してください")
        @Size(max = 255, message = "タイトルは255文字以内で入力してください") String title) {
}
