package com.example.todoapp.api;

import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.Map;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.format.annotation.DateTimeFormat.ISO;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class HolidayApiController {
    private final HolidayClient holidayClient;

    public HolidayApiController(HolidayClient holidayClient) {
        this.holidayClient = holidayClient;
    }

    @GetMapping("/api/holidays")
    public Map<String, String> holidays(
            @RequestParam(name = "from", required = false) @DateTimeFormat(iso = ISO.DATE) LocalDate from,
            @RequestParam(name = "to", required = false) @DateTimeFormat(iso = ISO.DATE) LocalDate to) {
        Map<String, String> holidays = holidayClient.fetchHolidays();
        if (from == null && to == null) {
            return holidays;
        }

        Map<String, String> filteredHolidays = new LinkedHashMap<>();
        holidays.forEach((date, name) -> {
            LocalDate holidayDate = LocalDate.parse(date);
            if ((from == null || !holidayDate.isBefore(from))
                    && (to == null || !holidayDate.isAfter(to))) {
                filteredHolidays.put(date, name);
            }
        });
        return filteredHolidays;
    }
}
