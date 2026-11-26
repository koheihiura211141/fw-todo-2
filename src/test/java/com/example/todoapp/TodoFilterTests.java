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
