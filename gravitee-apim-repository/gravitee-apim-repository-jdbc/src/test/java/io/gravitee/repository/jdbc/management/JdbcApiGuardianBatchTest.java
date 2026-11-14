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

import static org.assertj.core.api.SoftAssertions.assertSoftly;
import static org.mockito.Mockito.mockingDetails;
import static org.mockito.Mockito.spy;

import io.gravitee.repository.management.AbstractManagementRepositoryTest;
import io.gravitee.repository.management.api.search.ApiCriteria;
import io.gravitee.repository.management.api.search.ApiFieldFilter;
import io.gravitee.repository.management.model.Api;
import jakarta.inject.Inject;
import java.util.Arrays;
import java.util.Properties;
import java.util.Set;
import javax.sql.DataSource;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.PreparedStatementSetter;
import org.springframework.jdbc.core.ResultSetExtractor;

class JdbcApiGuardianBatchTest extends AbstractManagementRepositoryTest {

    @Inject
    private DataSource dataSource;

    @Inject
    private Properties graviteeProperties;

    @Override
    protected String getTestCasesPath() {
        return "/data/api-tests/";
    }

    @Test
    void should_load_all_guardian_associations_in_one_query_for_multiple_apis() {
        var template = spy(new JdbcTemplate(dataSource));
        var repository = new JdbcApiRepository(graviteeProperties.getProperty("management.jdbc.prefix", ""));
        repository.jdbcTemplate = template;

        var apis = repository.search(new ApiCriteria.Builder().build(), ApiFieldFilter.defaultFields());
        long guardianQueries = mockingDetails(template)
            .getInvocations()
            .stream()
            .filter(invocation ->
                Arrays.equals(
                    invocation.getMethod().getParameterTypes(),
                    new Class<?>[] { String.class, PreparedStatementSetter.class, ResultSetExtractor.class }
                )
            )
            .map(invocation -> invocation.getArguments()[0])
            .filter(argument -> argument instanceof String sql && sql.startsWith("select") && sql.contains("api_guardians"))
            .count();

        assertSoftly(softly -> {
            softly.assertThat(apis).as("search covers multiple APIs").hasSizeGreaterThan(1);
            softly.assertThat(guardianQueries).as("Guardian query count independent of result size").isEqualTo(1);
            softly
                .assertThat(apis)
                .filteredOn(api -> api.getId().equals("api-to-findById"))
                .singleElement()
                .extracting(Api::getGuardians)
                .as("every association retained")
                .isEqualTo(Set.of("guardian-1", "guardian-2"));
            softly.assertThat(apis.stream().map(Api::getGuardians)).as("APIs without Guardians have empty sets").contains(Set.of());
        });
    }
}
