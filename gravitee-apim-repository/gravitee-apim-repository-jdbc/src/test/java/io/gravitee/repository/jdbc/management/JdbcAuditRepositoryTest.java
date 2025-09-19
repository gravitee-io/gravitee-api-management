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
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import io.gravitee.common.data.domain.Page;
import io.gravitee.repository.management.api.search.AuditCriteria;
import io.gravitee.repository.management.api.search.builder.PageableBuilder;
import io.gravitee.repository.management.model.Audit;
import java.lang.reflect.Field;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;

class JdbcAuditRepositoryTest {

    @Test
    void shouldReturnPageWithCorrectPagination() throws Exception {
        JdbcAuditRepository repo = spy(new JdbcAuditRepository(""));
        JdbcTemplate jdbcTemplate = mock(JdbcTemplate.class);
        Field field = null;
        Class<?> clazz = repo.getClass();
        while (clazz != null) {
            try {
                field = clazz.getDeclaredField("jdbcTemplate");
                field.setAccessible(true);
                field.set(repo, jdbcTemplate);
                break;
            } catch (NoSuchFieldException e) {
                clazz = clazz.getSuperclass();
            }
        }
        if (field == null) throw new RuntimeException("jdbcTemplate field not found");
        when(jdbcTemplate.queryForObject(anyString(), any(Object[].class), eq(Long.class))).thenReturn(5L);

        doAnswer(invocation -> {
            JdbcHelper.CollatingRowMapper<Audit> rowMapper = invocation.getArgument(1);
            rowMapper.getRows().add(new Audit());
            rowMapper.getRows().add(new Audit());
            return null;
        })
            .when(jdbcTemplate)
            .query(anyString(), any(JdbcHelper.CollatingRowMapper.class), any(Object[].class));
        AuditCriteria criteria = new AuditCriteria.Builder().organizationId("org1").build();
        var pageable = new PageableBuilder().pageNumber(0).pageSize(2).build();

        Page<Audit> result = repo.search(criteria, pageable);
        assertThat(result.getTotalElements()).isEqualTo(5);
        assertThat(result.getPageNumber()).isEqualTo(0);

        verify(jdbcTemplate).queryForObject(anyString(), any(Object[].class), eq(Long.class));
        verify(jdbcTemplate).query(anyString(), any(JdbcHelper.CollatingRowMapper.class), any(Object[].class));
    }
}
