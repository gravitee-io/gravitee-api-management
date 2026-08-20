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
import static org.mockito.Mockito.when;

import io.gravitee.repository.exceptions.TechnicalException;
import java.lang.reflect.Field;
import lombok.SneakyThrows;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;

class JdbcPortalRepositoryTest {

    private final JdbcPortalRepository repository = new JdbcPortalRepository("");
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
    void findByActiveThemeIdAndEnvironmentId_wraps_exception_as_technical() {
        when(jdbcTemplate.query(anyString(), any(RowMapper.class), any(Object.class), any(Object.class))).thenThrow(
            new RuntimeException("boom")
        );

        var ex = catchThrowable(() -> repository.findByActiveThemeIdAndEnvironmentId("theme1", "env1"));

        assertThat(ex)
            .isInstanceOf(TechnicalException.class)
            .hasMessageContaining("Failed to find portals by active theme id (theme1)")
            .hasMessageContaining("environment id (env1)")
            .hasCauseInstanceOf(RuntimeException.class);
    }
}
