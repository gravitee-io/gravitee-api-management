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

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import java.lang.reflect.Field;
import java.util.List;
import lombok.SneakyThrows;
import org.junit.Test;
import org.springframework.jdbc.core.JdbcTemplate;

public class JdbcPageRepositoryTest {

    JdbcTemplate jdbcTemplate = mock(JdbcTemplate.class);

    @Test
    public void unsetHomepage() {
        JdbcPageRepository repository = buildRepository();

        // When
        repository.unsetHomepage(List.of("id1", "id2"));

        // Then
        verify(jdbcTemplate).update(eq("update from prefix_pages where page_id  in (? , ? ) set homepage = false"), eq("id1"), eq("id2"));
    }

    @SneakyThrows
    private JdbcPageRepository buildRepository() {
        JdbcPageRepository repository = new JdbcPageRepository("prefix_");
        Field field = JdbcAbstractRepository.class.getDeclaredField("jdbcTemplate");
        field.setAccessible(true);
        field.set(repository, jdbcTemplate);
        field.setAccessible(false);
        return repository;
    }
}
