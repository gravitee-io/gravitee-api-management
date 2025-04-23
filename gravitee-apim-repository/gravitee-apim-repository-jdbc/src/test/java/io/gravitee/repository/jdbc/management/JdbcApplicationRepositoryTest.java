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

import io.gravitee.repository.management.api.search.ApplicationCriteria;
import io.gravitee.repository.management.api.search.Order;
import io.gravitee.repository.management.api.search.Sortable;
import io.gravitee.repository.management.api.search.builder.SortableBuilder;
import io.gravitee.repository.management.model.ApplicationStatus;
import java.util.Set;
import org.junit.jupiter.api.Test;

public class JdbcApplicationRepositoryTest {

    @Test
    public void searchQuery_queryAndEnvironmentIdsAndStatus() {
        JdbcApplicationRepository repository = new JdbcApplicationRepository("table_prefix_");
        Sortable sortable = new SortableBuilder().field("name").order(Order.ASC).build();
        ApplicationCriteria criteria = ApplicationCriteria
            .builder()
            .query("id1")
            .environmentIds(Set.of("env1"))
            .status(ApplicationStatus.ACTIVE)
            .build();
        String query = repository.searchQuery(criteria, sortable);
        String expectedQuery =
            "select a.id, a.environment_id, a.name, a.description, a.type, a.created_at, " +
            "a.updated_at, a.status, a.disable_membership_notifications, a.api_key_mode, a.origin, " +
            "am.k as am_k, am.v as am_v from table_prefix_applications a left join " +
            "table_prefix_application_metadata am on a.id = am.application_id where 1 = 1 and " +
            "(a.id = ? or lower(a.name) like ?) and a.status = ? and a.environment_id in (? ) " +
            "order by a.name asc";
        assertThat(query).isEqualTo(expectedQuery);
    }

    @Test
    public void searchQuery_queryAndRestrictedIdsAndNameAndEnvironmentIdsAndStatus() {
        JdbcApplicationRepository repository = new JdbcApplicationRepository("table_prefix_");
        Sortable sortable = new SortableBuilder().field("name").order(Order.ASC).build();
        ApplicationCriteria criteria = ApplicationCriteria
            .builder()
            .query("name1")
            .restrictedToIds(Set.of("id1", "id2"))
            .name("name1")
            .environmentIds(Set.of("env1"))
            .status(ApplicationStatus.ACTIVE)
            .build();
        String query = repository.searchQuery(criteria, sortable);
        String expectedQuery =
            "select a.id, a.environment_id, a.name, a.description, a.type, a.created_at, " +
            "a.updated_at, a.status, a.disable_membership_notifications, a.api_key_mode, a.origin, " +
            "am.k as am_k, am.v as am_v from table_prefix_applications a left join " +
            "table_prefix_application_metadata am on a.id = am.application_id where 1 = 1 and " +
            "(a.id = ? or lower(a.name) like ?) and a.id in (? , ? ) and lower(a.name) like ? and a.status = ? " +
            "and a.environment_id in (? ) order by a.name asc";
        assertThat(query).isEqualTo(expectedQuery);
    }
}
