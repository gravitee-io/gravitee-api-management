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

import io.gravitee.repository.management.AbstractManagementRepositoryTest;
import io.gravitee.repository.management.model.Portal;
import jakarta.inject.Inject;
import java.util.Properties;
import javax.sql.DataSource;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;

/**
 * Rows written before the default marker and the URL existed get both from the column defaults: a JDBC specific test
 * inserting such a row directly, since the repository always writes both columns.
 *
 * @author GraviteeSource Team
 */
public class JdbcPortalStoredDefaultsTest extends AbstractManagementRepositoryTest {

    @Inject
    private DataSource dataSource;

    @Inject
    private Properties graviteeProperties;

    @Override
    protected String getTestCasesPath() {
        return null;
    }

    @Test
    public void should_read_a_portal_stored_without_default_marker_and_url_as_not_default_with_placeholder_url() throws Exception {
        new JdbcTemplate(dataSource).update(
            "insert into " +
                graviteeProperties.getProperty("management.jdbc.prefix", "") +
                "portals (id, environment_id, organization_id, name) values (?, ?, ?, ?)",
            "legacy-portal",
            "environment1",
            "organization1",
            "Legacy Portal"
        );

        assertThat(portalRepository.findById("legacy-portal")).hasValueSatisfying(found -> {
            assertThat(found.isDefaultPortal()).isFalse();
            assertThat(found.getUrl()).isEqualTo(Portal.URL_PLACEHOLDER);
        });
    }
}
