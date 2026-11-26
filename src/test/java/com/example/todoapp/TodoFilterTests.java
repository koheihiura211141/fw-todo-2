package com.example.todoapp;

import java.util.HashMap;
import java.util.Map;

import org.apache.ibatis.builder.xml.XMLMapperBuilder;
import org.apache.ibatis.io.Resources;
import org.apache.ibatis.session.Configuration;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class TodoFilterTests {
    @Test
    void sortingUsesAllowedColumnsAndStableTieBreaker() throws Exception {
        Configuration configuration = new Configuration();
        try (var stream = Resources.getResourceAsStream("mapper/TodoMapper.xml")) {
            new XMLMapperBuilder(stream, configuration, "mapper/TodoMapper.xml",
                    configuration.getSqlFragments()).parse();
        }
        var statement = configuration.getMappedStatement("com.example.todoapp.TodoMapper.searchPage");
        for (String field : new String[] {"title", "category", "priority", "completed", "deadline", "invalid"}) {
            for (String direction : new String[] {"asc", "desc"}) {
                String column = field.equals("deadline") || field.equals("invalid")
                        ? "(due_date IS NULL) ASC, due_date" : field;
                String sql = statement.getBoundSql(Map.of("sortBy", field, "order", direction))
                        .getSql().replaceAll("\\s+", " ");
                assertTrue(sql.contains("ORDER BY " + column + " " + direction.toUpperCase() + " , id ASC"), sql);
                assertTrue(sql.contains("LIMIT ? OFFSET ?"));
            }
        }
    }
    @Test
    void filtersApplyToBothRowsAndCountIncludingIncompleteStatus() throws Exception {
        Configuration configuration = new Configuration();
        try (var stream = Resources.getResourceAsStream("mapper/TodoMapper.xml")) {
            new XMLMapperBuilder(stream, configuration, "mapper/TodoMapper.xml",
                    configuration.getSqlFragments()).parse();
        }
        Map<String, Object> filters = new HashMap<>();
        filters.put("category", "デザイン");
        filters.put("priority", 1);
        filters.put("completed", false);
        for (String statement : new String[] {"searchPage", "countSearch"}) {
            var mapped = configuration.getMappedStatement("com.example.todoapp.TodoMapper." + statement);
            var bound = mapped.getBoundSql(filters);
            String sql = bound.getSql().replaceAll("\\s+", " ");
            assertTrue(sql.contains("category = ?"));
            assertTrue(sql.contains("priority = ?"));
            assertTrue(sql.contains("completed = ?"));
            filters.remove("priority");
            filters.remove("completed");
            String cleared = mapped.getBoundSql(filters).getSql();
            assertFalse(cleared.contains("AND priority ="));
            assertFalse(cleared.contains("AND completed ="));
            filters.put("priority", 1);
            filters.put("completed", false);
        }
    }
}
