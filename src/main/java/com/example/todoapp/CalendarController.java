package com.example.todoapp;

import java.time.DateTimeException;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.server.ResponseStatusException;

@Controller
public class CalendarController {

    @GetMapping("/calendar")
    public String calendar(@RequestParam(name = "year", required = false) Integer year,
                           @RequestParam(name = "month", required = false) Integer month,
                           Model model) {
        YearMonth now = YearMonth.now();
        YearMonth current;
        YearMonth previous;
        YearMonth next;
        try {
            current = YearMonth.of(year == null ? now.getYear() : year,
                                   month == null ? now.getMonthValue() : month);
            previous = current.minusMonths(1);
            next = current.plusMonths(1);
        } catch (DateTimeException ex) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid year or month", ex);
        }

        LocalDate firstDay = current.atDay(1);
        LocalDate lastDay = current.atEndOfMonth();
        int leadingBlanks = firstDay.getDayOfWeek().getValue() % 7;
        int weekCount = (leadingBlanks + current.lengthOfMonth() + 6) / 7;
        List<List<LocalDate>> weeks = new ArrayList<>();
        for (int week = 0; week < weekCount; week++) {
            List<LocalDate> days = new ArrayList<>();
            for (int weekday = 0; weekday < 7; weekday++) {
                int day = week * 7 + weekday - leadingBlanks + 1;
                days.add(day >= 1 && day <= current.lengthOfMonth() ? current.atDay(day) : null);
            }
            weeks.add(days);
        }

        model.addAttribute("year", current.getYear());
        model.addAttribute("month", current.getMonthValue());
        model.addAttribute("firstDay", firstDay);
        model.addAttribute("lastDay", lastDay);
        model.addAttribute("previousMonth", previous);
        model.addAttribute("nextMonth", next);
        model.addAttribute("weeks", weeks);
        return "calendar";
    }
}
