package com.example.todoapp;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Set;
import java.util.stream.Collectors;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class SampleTodoServiceTests {
    @Test void firstLoginCreatesFifteenVariedTodosOwnedByUser() {
        var samples = mock(SampleTodoMapper.class);
        var todos = mock(TodoMapper.class);
        when(samples.lockUser(7L)).thenReturn(7L);
        LocalDate before = LocalDate.now(ZoneId.of("Asia/Tokyo"));
        new SampleTodoService(samples, todos).initialize(7L);

        var captor = ArgumentCaptor.forClass(Todo.class);
        verify(todos, times(15)).insert(captor.capture());
        var data = captor.getAllValues();
        assertTrue(data.stream().allMatch(t -> t.getUserId().equals(7L)));
        assertEquals(15, data.stream().map(Todo::getTitle).distinct().count());
        assertEquals(Set.of("デザイン", "マーケティング", "プログラミング", "資格", "就職活動", "その他"),
                data.stream().map(Todo::getCategory).collect(Collectors.toSet()));
        assertEquals(Set.of(1, 2, 3), data.stream().map(Todo::getPriority).collect(Collectors.toSet()));
        assertEquals(Set.of(true, false), data.stream().map(Todo::getCompleted).collect(Collectors.toSet()));
        assertTrue(data.stream().anyMatch(t -> t.getDeadline() == null));
        assertTrue(data.stream().anyMatch(t -> t.getDeadline() != null && t.getDeadline().isBefore(before)));
        assertTrue(data.stream().anyMatch(t -> t.getDeadline() != null && t.getDeadline().isAfter(before)));
        // Match the default deadline ordering: the first page must show all three visual states.
        LocalDate seededToday = data.stream().filter(t -> t.getTitle().equals("Javaの例外処理を復習する"))
                .findFirst().orElseThrow().getDeadline();
        var firstPage = data.stream().sorted(java.util.Comparator.comparing(Todo::getDeadline,
                java.util.Comparator.nullsLast(java.util.Comparator.naturalOrder()))).limit(10).toList();
        assertEquals(1, firstPage.stream().filter(t -> t.getDeadline().isBefore(seededToday)).count());
        assertEquals(2, firstPage.stream().filter(t -> !t.getDeadline().isBefore(seededToday)
                && !t.getDeadline().isAfter(seededToday.plusDays(3))).count());
        assertEquals(7, firstPage.stream().filter(t -> t.getDeadline().isAfter(seededToday.plusDays(3))).count());
        var order = inOrder(samples, todos);
        order.verify(samples).lockUser(7L);
        order.verify(samples).countInitialization(7L);
        order.verify(todos, times(15)).insert(any());
        order.verify(samples).markInitialized(7L);
    }

    @Test void subsequentLoginDoesNotRecreateDeletedSamples() {
        var samples = mock(SampleTodoMapper.class);
        var todos = mock(TodoMapper.class);
        when(samples.lockUser(7L)).thenReturn(7L);
        when(samples.countInitialization(7L)).thenReturn(1);
        new SampleTodoService(samples, todos).initialize(7L);
        verifyNoInteractions(todos);
        verify(samples, never()).markInitialized(any());
    }

    @Test void insertFailureDoesNotMarkInitializationComplete() {
        var samples = mock(SampleTodoMapper.class);
        var todos = mock(TodoMapper.class);
        when(samples.lockUser(7L)).thenReturn(7L);
        when(todos.insert(any())).thenThrow(new IllegalStateException("insert failed"));
        assertThrows(IllegalStateException.class, () -> new SampleTodoService(samples, todos).initialize(7L));
        verify(samples, never()).markInitialized(any());
    }
}
