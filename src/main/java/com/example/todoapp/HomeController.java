package com.example.todoapp;

import java.time.LocalDate;
import java.util.List;

import org.springframework.stereotype.Controller;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
public class HomeController {

    private final TodoService todoService;

    public HomeController(TodoService todoService) {
        this.todoService = todoService;
    }

    @ModelAttribute("currentUser")
    public LoginUser currentUser(@org.springframework.security.core.annotation.AuthenticationPrincipal LoginUser user) {
        return user;
    }

    @GetMapping("/")
    public String index(Model model) {
        model.addAttribute("title", "やること管理");
        return "index";
    }

    @GetMapping("/todos/kanban")
    public String kanban(Model model) {
        model.addAttribute("todos", todoService.search("", "", "asc",
                PageRequest.of(0, Integer.MAX_VALUE)).getContent());
        model.addAttribute("statuses", TodoStatus.values());
        return "kanban";
    }

    @GetMapping("/todos")
    public String todos(@RequestParam(defaultValue = "") String keyword,
                        @RequestParam(defaultValue = "") String category,
                        @RequestParam(defaultValue = "asc") String order,
                        @RequestParam(defaultValue = "deadline") String sortBy,
                        @RequestParam(required = false) Integer priority,
                        @RequestParam(required = false) Boolean completed,
                        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
                        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
                        @PageableDefault(size = 10) Pageable pageable,
                        Model model) {
        if (!order.equals("desc")) {
            order = "asc";
        }
        if (!List.of("title", "category", "priority", "deadline", "completed").contains(sortBy)) {
            sortBy = "deadline";
        }
        Sort sort = Sort.by(sortBy);
        int pageNumber = Math.max(0, pageable.getPageNumber());
        pageable = PageRequest.of(pageNumber, 10, sort);
        Page<Todo> todoPage = todoService.search(keyword, category, order, priority, completed, from, to, pageable);
        if (todoPage.getTotalPages() > 0 && pageNumber >= todoPage.getTotalPages()) {
            pageNumber = todoPage.getTotalPages() - 1;
            pageable = PageRequest.of(pageNumber, 10, sort);
            todoPage = todoService.search(keyword, category, order, priority, completed, from, to, pageable);
        }
        model.addAttribute("todoPage", todoPage);
        model.addAttribute("todos", todoPage.getContent());
        LocalDate today = LocalDate.now(java.time.ZoneId.of("Asia/Tokyo"));
        model.addAttribute("today", today);
        model.addAttribute("warningDate", today.plusDays(3));
        model.addAttribute("startItem", todoPage.getTotalElements() == 0 ? 0 : pageable.getOffset() + 1);
        model.addAttribute("endItem", pageable.getOffset() + todoPage.getNumberOfElements());
        model.addAttribute("keyword", keyword);
        model.addAttribute("category", category);
        model.addAttribute("order", order);
        model.addAttribute("sortBy", sortBy);
        model.addAttribute("priority", priority);
        model.addAttribute("completed", completed);
        model.addAttribute("from", from);
        model.addAttribute("to", to);
        return "todos";
    }

    @PostMapping("/todos")
    public String createTodo(@ModelAttribute TodoForm form, RedirectAttributes redirectAttributes) {
        todoService.create(form.toTodo());
        redirectAttributes.addFlashAttribute("message", "登録しました");
        return "redirect:/todos";
    }

    @GetMapping("/todos/new")
    public String newTodo(Model model) {
        model.addAttribute("todo", new TodoForm());
        return "create";
    }

    @PostMapping("/todos/confirm")
    public String confirmTodo(@Valid @ModelAttribute("todo") TodoForm todo, BindingResult bindingResult, Model model) {
        if (bindingResult.hasErrors()) {
            return "create";
        }
        model.addAttribute("todo", todo);
        return "create-confirm";
    }

    @PostMapping("/todos/new")
    public String rewriteTodo(@ModelAttribute("todo") TodoForm todo, Model model) {
        model.addAttribute("todo", todo);
        return "create";
    }

    @GetMapping("/todos/{id}/edit")
    public String editTodo(@PathVariable Long id, Model model, RedirectAttributes redirectAttributes) {
        Todo todo = todoService.findById(id);
        if (todo == null) {
            redirectAttributes.addFlashAttribute("message", "見つかりませんでした");
            return "redirect:/todos";
        }
        model.addAttribute("todo", TodoForm.fromTodo(todo));
        model.addAttribute("todoId", id);
        return "edit";
    }

    @PostMapping("/todos/{id}/confirm")
    public String confirmEditTodo(@PathVariable Long id, @Valid @ModelAttribute("todo") TodoForm todo, BindingResult bindingResult, Model model) {
        todoService.findById(id);
        if (bindingResult.hasErrors()) {
            model.addAttribute("todoId", id);
            return "edit";
        }
        model.addAttribute("todo", todo);
        model.addAttribute("todoId", id);
        return "edit-confirm";
    }

    @PostMapping("/todos/{id}")
    public String updateTodo(@PathVariable Long id, @ModelAttribute("todo") TodoForm form, RedirectAttributes redirectAttributes) {
        Todo todo = form.toTodo();
        todo.setId(id);
        todoService.update(todo);
        redirectAttributes.addFlashAttribute("message", "保存しました");
        return "redirect:/todos";
    }

    @PostMapping("/todos/{id}/edit")
    public String rewriteEditTodo(@PathVariable Long id, @ModelAttribute("todo") TodoForm todo, Model model) {
        todoService.findById(id);
        model.addAttribute("todo", todo);
        model.addAttribute("todoId", id);
        return "edit";
    }
    @GetMapping("/todos/{id}/delete")
    public String confirmDeleteTodo(@PathVariable Long id, Model model, RedirectAttributes redirectAttributes) {
        Todo todo = todoService.findById(id);
        if (todo == null) {
            redirectAttributes.addFlashAttribute("message", "見つかりませんでした");
            return "redirect:/todos";
        }
        model.addAttribute("todo", todo);
        return "delete";
    }

    @PostMapping("/todos/bulk-delete")
    public String deleteSelectedTodos(@RequestParam(name = "ids", required = false) List<Long> ids,
                                      RedirectAttributes redirectAttributes) {
        int deleted = todoService.deleteAll(ids);
        redirectAttributes.addFlashAttribute("message", deleted > 0
                ? deleted + "件削除しました" : "削除対象が選択されていないか、既に削除されています");
        return "redirect:/todos";
    }

    @PostMapping("/todos/{id}/delete")
    public String deleteTodo(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        todoService.delete(id);
        redirectAttributes.addFlashAttribute("message", "削除しました");
        return "redirect:/todos";
    }
}
