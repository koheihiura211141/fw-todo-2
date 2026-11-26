package com.example.todoapp;

import java.time.LocalDate;

import org.springframework.format.annotation.DateTimeFormat;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

/** Form object used by the create and edit screens. */
@Data
public class TodoForm {
    @NotBlank(message = "作成者を入力してください")
    @Size(max = 50, message = "作成者は50文字以内で入力してください")
    private String author;

    @NotBlank(message = "タイトルを入力してください")
    @Size(max = 100, message = "タイトルは100文字以内で入力してください")
    private String title;

    @Size(max = 500, message = "詳細は500文字以内で入力してください")
    private String detail;

    @NotBlank(message = "ジャンルを選択してください")
    private String category;
    @NotNull(message = "優先度を選択してください")
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

    public Todo toTodo() {
        Todo todo = new Todo();
        todo.setTitle(title);
        todo.setDetail(detail);
        todo.setCategory(category);
        todo.setPriority(priority);
        todo.setDeadline(deadline);
        todo.setCompleted(completed);
        return todo;
    }

    public static TodoForm fromTodo(Todo todo) {
        TodoForm form = new TodoForm();
        form.setTitle(todo.getTitle());
        form.setDetail(todo.getDetail());
        form.setCategory(todo.getCategory());
        form.setPriority(todo.getPriority());
        form.setDeadline(todo.getDeadline());
        form.setCompleted(todo.getCompleted());
        return form;
    }
}
