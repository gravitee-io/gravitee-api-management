/*
 * Copyright © 2015 The Gravitee team (http://gravitee.io)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.gravitee.repository.jdbc.management;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.startsWith;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.gravitee.repository.management.model.Command;
import java.lang.reflect.Field;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InOrder;
import org.springframework.jdbc.core.BatchPreparedStatementSetter;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.PreparedStatementCreator;

/**
 * {@code delete} and {@code deleteByExpiredAtBefore} are called concurrently by two independently
 * scheduled services. They must take their row locks on {@code command_acknowledgments},
 * {@code command_tags} and {@code commands} in the same order, otherwise PostgreSQL kills one of the
 * two transactions with a deadlock.
 */
class JdbcCommandRepositoryTest {

    private JdbcCommandRepository cut;
    private JdbcTemplate jdbcTemplate;

    @BeforeEach
    void setUp() throws Exception {
        cut = new JdbcCommandRepository("");
        jdbcTemplate = mock(JdbcTemplate.class);

        Field field = null;
        Class<?> clazz = cut.getClass();
        while (clazz != null && field == null) {
            try {
                field = clazz.getDeclaredField("jdbcTemplate");
                field.setAccessible(true);
                field.set(cut, jdbcTemplate);
            } catch (NoSuchFieldException e) {
                clazz = clazz.getSuperclass();
            }
        }
        if (field == null) {
            throw new IllegalStateException("jdbcTemplate field not found");
        }
    }

    @Test
    void should_delete_the_child_rows_before_the_command() throws Exception {
        cut.delete("command-id");

        InOrder inOrder = inOrder(jdbcTemplate);
        inOrder.verify(jdbcTemplate).update(startsWith("delete from command_acknowledgments"), eq("command-id"));
        inOrder.verify(jdbcTemplate).update(startsWith("delete from command_tags"), eq("command-id"));
        inOrder.verify(jdbcTemplate).update(eq(cut.getOrm().getDeleteSql()), eq("command-id"));
    }

    @Test
    void should_delete_the_child_rows_before_the_expired_commands() throws Exception {
        cut.deleteByExpiredAtBefore(Instant.now());

        InOrder inOrder = inOrder(jdbcTemplate);
        inOrder.verify(jdbcTemplate).update(startsWith("delete from command_acknowledgments"), any(Object.class));
        inOrder.verify(jdbcTemplate).update(startsWith("delete from command_tags"), any(Object.class));
        inOrder.verify(jdbcTemplate).update(startsWith("delete from commands where expired_at"), any(Object.class));
    }

    /**
     * {@code update} rewrites the same child rows and runs concurrently with the two delete methods, so it has to
     * take the locks in that same order. It used to touch the command row first, which left a deadlock between
     * {@code update} and {@code delete}.
     */
    @Test
    void should_rewrite_the_child_rows_before_the_command() {
        when(jdbcTemplate.queryForList(startsWith("select acknowledgment"), eq(String.class), eq("command-id"))).thenReturn(
            List.of("removed-ack")
        );
        when(jdbcTemplate.queryForList(startsWith("select tag"), eq(String.class), eq("command-id"))).thenReturn(List.of("removed-tag"));
        Command command = new Command();
        command.setId("command-id");

        // findById cannot resolve against a mocked template, so update ends on its IllegalStateException.
        // Everything under assertion below has already run by then.
        assertThrows(IllegalStateException.class, () -> cut.update(command));

        InOrder inOrder = inOrder(jdbcTemplate);
        inOrder.verify(jdbcTemplate).update(startsWith("delete from command_acknowledgments"), eq("command-id"), eq("removed-ack"));
        inOrder.verify(jdbcTemplate).update(startsWith("delete from command_tags"), eq("command-id"), eq("removed-tag"));
        inOrder.verify(jdbcTemplate).update(any(PreparedStatementCreator.class));
    }

    @Test
    void should_not_rewrite_the_child_rows_that_did_not_change() {
        when(jdbcTemplate.queryForList(startsWith("select acknowledgment"), eq(String.class), eq("command-id"))).thenReturn(
            List.of("node-1")
        );
        when(jdbcTemplate.queryForList(startsWith("select tag"), eq(String.class), eq("command-id"))).thenReturn(List.of("DATA_TO_INDEX"));
        Command command = new Command();
        command.setId("command-id");
        command.setAcknowledgments(List.of("node-1", "node-2"));
        command.setTags(List.of("DATA_TO_INDEX"));

        assertThrows(IllegalStateException.class, () -> cut.update(command));

        verify(jdbcTemplate, never()).update(startsWith("delete from"), any(Object[].class));
        verify(jdbcTemplate).batchUpdate(startsWith("insert into command_acknowledgments"), any(BatchPreparedStatementSetter.class));
        verify(jdbcTemplate, never()).batchUpdate(startsWith("insert into command_tags"), any(BatchPreparedStatementSetter.class));
    }
}
