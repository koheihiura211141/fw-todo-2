package com.example.todoapp;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.ibatis.builder.xml.XMLMapperBuilder;
import org.apache.ibatis.io.Resources;
import org.apache.ibatis.session.Configuration;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class TodoBulkDeleteTests {
    @Test
    void bindsRepeatedIdsAndDeletesThemInOneCall() throws Exception {
        TodoMapper mapper = mock(TodoMapper.class);
        when(mapper.deleteByIds(List.of(2L, 5L))).thenReturn(2);
        var mvc = MockMvcBuilders.standaloneSetup(new HomeController(new TodoService(mapper))).build();

        mvc.perform(post("/todos/bulk-delete").param("ids", "2", "5"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/todos"))
                .andExpect(flash().attribute("message", "2件削除しました"));
        verify(mapper).deleteByIds(List.of(2L, 5L));
        verifyNoMoreInteractions(mapper);
    }

    @Test
    void missingOrEmptyIdsNeverReachMapper() throws Exception {
        TodoMapper mapper = mock(TodoMapper.class);
        TodoService service = new TodoService(mapper);
        var mvc = MockMvcBuilders.standaloneSetup(new HomeController(service)).build();

        mvc.perform(post("/todos/bulk-delete"))
                .andExpect(status().is3xxRedirection()).andExpect(redirectedUrl("/todos"));
        assertEquals(0, service.deleteAll(List.of()));
        verifyNoInteractions(mapper);
    }

    @Test
    void invalidIdsAreRejectedBeforeDeletion() throws Exception {
        TodoMapper mapper = mock(TodoMapper.class);
        var mvc = MockMvcBuilders.standaloneSetup(new HomeController(new TodoService(mapper))).build();
        mvc.perform(post("/todos/bulk-delete").param("ids", "2", "invalid"))
                .andExpect(status().isBadRequest());
        verifyNoInteractions(mapper);
    }

    @Test
    void sqlUsesBoundInParametersAndEmptyListsCannotDeleteRows() throws Exception {
        Configuration configuration = new Configuration();
        try (var stream = Resources.getResourceAsStream("mapper/TodoMapper.xml")) {
            new XMLMapperBuilder(stream, configuration, "mapper/TodoMapper.xml",
                    configuration.getSqlFragments()).parse();
        }
        var statement = configuration.getMappedStatement("com.example.todoapp.TodoMapper.deleteByIds");
        var bound = statement.getBoundSql(Map.of("ids", List.of(2L, 5L)));
        assertEquals("DELETE FROM todos WHERE id IN ( ? , ? )", bound.getSql().replaceAll("\\s+", " ").trim());
        assertEquals(2, bound.getParameterMappings().size());
        for (List<Long> ids : java.util.Arrays.asList(List.<Long>of(), null)) {
            Map<String, Object> parameters = new HashMap<>();
            parameters.put("ids", ids);
            assertEquals("DELETE FROM todos WHERE 1 = 0",
                    statement.getBoundSql(parameters).getSql().replaceAll("\\s+", " ").trim());
        }
    }
}
