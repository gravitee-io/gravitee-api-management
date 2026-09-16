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

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import io.gravitee.definition.model.dictionary.DictionaryProperty;
import io.gravitee.repository.management.model.Dictionary;
import java.lang.reflect.Field;
import java.sql.PreparedStatement;
import java.util.HashMap;
import java.util.Map;
import lombok.SneakyThrows;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.jdbc.core.BatchPreparedStatementSetter;
import org.springframework.jdbc.core.JdbcTemplate;

class JdbcDictionaryRepositoryTest {

    private final JdbcDictionaryRepository repository = new JdbcDictionaryRepository("");
    private JdbcTemplate jdbcTemplate;

    @BeforeEach
    @SneakyThrows
    void setUp() {
        jdbcTemplate = mock(JdbcTemplate.class);
        Field field = JdbcAbstractRepository.class.getDeclaredField("jdbcTemplate");
        field.setAccessible(true);
        field.set(repository, jdbcTemplate);
        field.setAccessible(false);
    }

    @Test
    @SneakyThrows
    void should_skip_a_null_property_entry_instead_of_failing_the_write() {
        Map<String, DictionaryProperty> properties = new HashMap<>();
        properties.put("valid", new DictionaryProperty("a-value", false));
        properties.put("malformed", null);

        Dictionary dictionary = new Dictionary();
        dictionary.setId("dictionary-id");
        dictionary.setProperties(properties);

        Throwable thrown = catchThrowable(() -> repository.create(dictionary));

        assertThat(thrown).isNull();

        ArgumentCaptor<BatchPreparedStatementSetter> setter = ArgumentCaptor.forClass(BatchPreparedStatementSetter.class);
        verify(jdbcTemplate).batchUpdate(anyString(), setter.capture());

        assertThat(setter.getValue().getBatchSize()).isEqualTo(1);

        PreparedStatement statement = mock(PreparedStatement.class);
        setter.getValue().setValues(statement, 0);
        verify(statement).setString(2, "valid");
        verify(statement).setString(3, "a-value");
        verify(statement).setBoolean(4, false);
    }

    @Test
    @SneakyThrows
    void should_not_batch_at_all_when_every_property_entry_is_null() {
        Map<String, DictionaryProperty> properties = new HashMap<>();
        properties.put("malformed", null);

        Dictionary dictionary = new Dictionary();
        dictionary.setId("dictionary-id");
        dictionary.setProperties(properties);

        Throwable thrown = catchThrowable(() -> repository.create(dictionary));

        assertThat(thrown).isNull();
        verify(jdbcTemplate, never()).batchUpdate(anyString(), any(BatchPreparedStatementSetter.class));
    }
}
