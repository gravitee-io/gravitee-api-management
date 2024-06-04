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
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import io.gravitee.repository.jdbc.orm.JdbcObjectMapper;
import io.gravitee.repository.management.api.search.ApiCriteria;
import io.gravitee.repository.management.model.Api;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.Random;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class JdbcApiRepositoryTest {

    static Random RANDOM = new Random();

    @Nested
    class MappingCriteriaToQuery {

        @Test
        void should_map_filter_name_as_lower_like() {
            // Given
            JdbcObjectMapper<Api> orm = JdbcObjectMapper.builder(Api.class, "mytable").build();
            ApiCriteria criteria = new ApiCriteria.Builder().filterName("my-name").build();

            // When
            String result = JdbcApiRepository.convert(criteria, orm);

            // Then
            assertThat(result).isEqualTo("LOWER( a.name ) LIKE ?");
        }
    }

    @Nested
    class FillParametersToPreparedStatement {

        @Test
        void should_map_filter_name_as_lower_like() throws SQLException {
            // Given
            JdbcObjectMapper<Api> orm = JdbcObjectMapper.builder(Api.class, "mytable").build();
            ApiCriteria criteria = new ApiCriteria.Builder().filterName("mY-naMe").build();
            PreparedStatement ps = mock(PreparedStatement.class);
            int lastIdx = RANDOM.nextInt(1000);

            // When
            int newLastIdx = JdbcApiRepository.fillPreparedStatement(criteria, ps, lastIdx, orm);

            // Then
            assertThat(newLastIdx).isEqualTo(lastIdx + 1);
            verify(ps).setString(eq(lastIdx), eq("%my-name%"));
        }
    }
}
