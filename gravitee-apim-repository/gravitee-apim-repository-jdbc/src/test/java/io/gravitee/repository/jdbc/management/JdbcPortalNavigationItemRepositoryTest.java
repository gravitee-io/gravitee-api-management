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
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import io.gravitee.repository.management.api.search.PortalNavigationItemCriteria;
import io.gravitee.repository.management.model.PortalNavigationItem;
import io.gravitee.repository.management.model.PortalNavigationReferenceType;
import java.lang.reflect.Field;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.PreparedStatementSetter;
import org.springframework.jdbc.core.RowCallbackHandler;
import org.springframework.jdbc.core.RowMapper;

class JdbcPortalNavigationItemRepositoryTest {

    private final JdbcPortalNavigationItemRepository repository = new JdbcPortalNavigationItemRepository("prefix_");
    private final JdbcTemplate jdbcTemplate = mock(JdbcTemplate.class);

    @BeforeEach
    void set_up() throws Exception {
        Field field = JdbcAbstractRepository.class.getDeclaredField("jdbcTemplate");
        field.setAccessible(true);
        field.set(repository, jdbcTemplate);
        field.setAccessible(false);
    }

    @ParameterizedTest
    @ValueSource(ints = { 500, 501, 1001 })
    void should_enrich_categories_across_bounded_queries_when_api_search_returns_many_listings(int itemCount) throws Exception {
        var apiIds = IntStream.range(0, 500)
            .mapToObj(index -> "api-" + index)
            .collect(Collectors.toSet());
        var items = IntStream.range(0, itemCount)
            .mapToObj(index ->
                PortalNavigationItem.builder()
                    .id("navigation-" + index)
                    .environmentId("env-id")
                    .referenceId("portal-id")
                    .rootId("navigation-" + index)
                    .type(PortalNavigationItem.Type.API)
                    .apiId("api-" + (index % 500))
                    .categoryIds(List.of("stale-category"))
                    .build()
            )
            .toList();
        Map<String, List<String>> categoriesByItemId = Map.of(
            "navigation-0",
            List.of("first-category", "another-category"),
            "navigation-499",
            List.of("last-in-first-batch"),
            "navigation-500",
            List.of("first-in-second-batch"),
            "navigation-999",
            List.of("last-in-second-batch"),
            "navigation-1000",
            List.of("first-in-third-batch")
        );
        when(jdbcTemplate.query(anyString(), any(PreparedStatementSetter.class), any(RowMapper.class))).thenReturn(items);
        var queriedIds = new ArrayList<String>();
        doAnswer(invocation -> {
            Object[] ids = (Object[]) invocation.getRawArguments()[2];
            assertThat(ids.length).isLessThanOrEqualTo(500);
            RowCallbackHandler callback = invocation.getArgument(1);
            for (Object id : ids) {
                var itemId = (String) id;
                queriedIds.add(itemId);
                for (var categoryId : categoriesByItemId.getOrDefault(itemId, List.of())) {
                    var resultSet = mock(ResultSet.class);
                    when(resultSet.getString("nav_item_id")).thenReturn(itemId);
                    when(resultSet.getString("category_id")).thenReturn(categoryId);
                    callback.processRow(resultSet);
                }
            }
            return null;
        })
            .when(jdbcTemplate)
            .query(contains("portal_navigation_item_categories"), any(RowCallbackHandler.class), any(Object[].class));
        var criteria = PortalNavigationItemCriteria.builder()
            .environmentId("env-id")
            .referenceType(PortalNavigationReferenceType.PORTAL)
            .referenceId("portal-id")
            .type(PortalNavigationItem.Type.API.name())
            .apiIds(apiIds)
            .build();

        var result = repository.searchByCriteria(criteria);

        assertThat(result).containsExactlyInAnyOrderElementsOf(items);
        assertThat(queriedIds).containsExactlyInAnyOrderElementsOf(items.stream().map(PortalNavigationItem::getId).toList());
        result.forEach(item ->
            assertThat(item.getCategoryIds()).containsExactlyInAnyOrderElementsOf(categoriesByItemId.getOrDefault(item.getId(), List.of()))
        );
    }
}
