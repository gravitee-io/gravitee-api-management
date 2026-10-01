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
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.gravitee.definition.model.dictionary.DictionaryProperty;
import io.gravitee.repository.management.model.Dictionary;
import io.gravitee.repository.management.model.DictionaryEncryptionPolicy;
import io.gravitee.repository.management.model.DictionaryType;
import java.lang.reflect.Field;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
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

    @Test
    @SneakyThrows
    void should_write_the_encryption_policy_to_its_own_column() {
        Dictionary dictionary = new Dictionary();
        dictionary.setId("dictionary-id");
        dictionary.setType(DictionaryType.DYNAMIC);
        DictionaryEncryptionPolicy encryption = new DictionaryEncryptionPolicy();
        encryption.setEncryptOnFetch(true);
        dictionary.setEncryption(encryption);

        Connection connection = mock(Connection.class);
        PreparedStatement statement = mock(PreparedStatement.class);
        when(connection.prepareStatement(anyString())).thenReturn(statement);

        repository.buildInsertPreparedStatementCreator(dictionary).createPreparedStatement(connection);

        verify(statement).setBoolean(anyInt(), eq(true));
    }

    @Test
    @SneakyThrows
    void should_write_false_when_a_dictionary_has_no_encryption_policy() {
        Dictionary dictionary = new Dictionary();
        dictionary.setId("dictionary-id");
        dictionary.setType(DictionaryType.MANUAL);

        Connection connection = mock(Connection.class);
        PreparedStatement statement = mock(PreparedStatement.class);
        when(connection.prepareStatement(anyString())).thenReturn(statement);

        Throwable thrown = catchThrowable(() ->
            repository.buildInsertPreparedStatementCreator(dictionary).createPreparedStatement(connection)
        );

        assertThat(thrown).isNull();
        verify(statement).setBoolean(anyInt(), eq(false));
    }

    @Test
    @SneakyThrows
    void should_read_the_encryption_policy_back_from_its_column() {
        ResultSet resultSet = mock(ResultSet.class);
        when(resultSet.getObject("type")).thenReturn("DYNAMIC");
        when(resultSet.getBoolean("encrypt_on_fetch")).thenReturn(true);

        Dictionary dictionary = repository.getRowMapper().mapRow(resultSet, 0);

        assertThat(dictionary.getEncryption().isEncryptOnFetch()).isTrue();
    }

    @Test
    @SneakyThrows
    void should_not_read_an_encryption_policy_for_a_manual_dictionary() {
        ResultSet resultSet = mock(ResultSet.class);
        when(resultSet.getObject("type")).thenReturn("MANUAL");
        when(resultSet.getBoolean("encrypt_on_fetch")).thenReturn(false);

        Dictionary dictionary = repository.getRowMapper().mapRow(resultSet, 0);

        assertThat(dictionary.getEncryption()).isNull();
    }
}
