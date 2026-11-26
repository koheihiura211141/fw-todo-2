package com.example.todoapp;

import java.time.LocalDate;
import java.time.LocalDateTime;

import org.springframework.format.annotation.DateTimeFormat;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import lombok.Data;

@Data
public class Todo {
    private Long id;
    private Long userId;
    @NotBlank(message = "やることを入力してください")
    @Size(max = 255, message = "やることは255文字以内で入力してください")
    private String title;
    @Size(max = 255, message = "メモは255文字以内で入力してください")
    private String detail;

    public String getDetailPreview() {
        return detail == null ? "" : detail.strip().replaceFirst("(?s)^([^。！？!?\\r\\n]*[。！？!?]?).*$", "$1");
    }
    @NotBlank(message = "ジャンルを選んでください")
    private String category;
    @NotNull(message = "優先度を選んでください")
    private Integer priority;

    @DateTimeFormat(pattern = "yyyy-MM-dd")
    private LocalDate deadline;

    /** Alias for existing API clients and calendar integration. */
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    public LocalDate getDueDate() {
        return deadline;
    }

    public void setDueDate(LocalDate dueDate) {
        this.deadline = dueDate;
    }

    private Boolean completed;
    private TodoStatus status;

    public TodoStatus getStatus() {
        if (Boolean.TRUE.equals(completed)) return TodoStatus.DONE;
        return status == TodoStatus.IN_PROGRESS ? status : TodoStatus.NOT_STARTED;
    }
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
