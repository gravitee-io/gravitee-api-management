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

import static io.gravitee.repository.jdbc.common.AbstractJdbcRepositoryConfiguration.escapeReservedWord;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.gravitee.repository.exceptions.TechnicalException;
import io.gravitee.repository.management.api.search.PortalNotificationCriteria;
import io.gravitee.repository.management.model.NotificationReferenceType;
import io.gravitee.repository.management.model.PortalNotificationConfig;
import java.lang.reflect.Field;
import java.util.List;
import java.util.Set;
import lombok.SneakyThrows;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowCallbackHandler;
import org.springframework.jdbc.core.RowMapper;

public class JdbcPortalNotificationConfigRepositoryTest {

    private JdbcPortalNotificationConfigRepository repository = new JdbcPortalNotificationConfigRepository("table_prefix_");
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
    public void generateQuery_withHookAndOrgId() {
        PortalNotificationCriteria criteria = PortalNotificationCriteria.builder().hook("hook1").organizationId("org1").build();
        String query = repository.generateQuery(criteria);
        String expectedQuery =
            "select pnc." +
            escapeReservedWord("user") +
            ", pnc.reference_type, pnc.reference_id, pnc.created_at, pnc.updated_at, pnc.origin from " +
            "table_prefix_portal_notification_configs pnc left join table_prefix_portal_notification_config_hooks pnch on " +
            "pnc.reference_type = pnch.reference_type and pnc.reference_id = pnch.reference_id and pnc." +
            escapeReservedWord("user") +
            " = pnch." +
            escapeReservedWord("user") +
            " where 1=1 and pnch.hook = ? and pnc.organization_id = ?";
        assertThat(query).isEqualTo(expectedQuery);
    }

    @Test
    public void generateQuery_withHookAndReferenceTypeAndReferenceId() {
        PortalNotificationCriteria criteria = PortalNotificationCriteria.builder()
            .hook("hook1")
            .referenceType(NotificationReferenceType.API)
            .referenceId("refId1")
            .build();
        String query = repository.generateQuery(criteria);
        String expectedQuery =
            "select pnc." +
            escapeReservedWord("user") +
            ", pnc.reference_type, pnc.reference_id, pnc.created_at, pnc.updated_at, pnc.origin from " +
            "table_prefix_portal_notification_configs pnc left join table_prefix_portal_notification_config_hooks pnch on " +
            "pnc.reference_type = pnch.reference_type and pnc.reference_id = pnch.reference_id and pnc." +
            escapeReservedWord("user") +
            " = pnch." +
            escapeReservedWord("user") +
            " where 1=1 and pnc.reference_type = ? and pnc.reference_id = ? and pnch.hook = ?";
        assertThat(query).isEqualTo(expectedQuery);
    }

    @Test
    void findAll_shouldLoadHooksAndGroups() throws Exception {
        PortalNotificationConfig config = PortalNotificationConfig.builder()
            .user("user1")
            .referenceType(NotificationReferenceType.API)
            .referenceId("api1")
            .build();

        when(jdbcTemplate.query(contains("FROM table_prefix_portal_notification_configs"), any(RowMapper.class))).thenReturn(
            List.of(config)
        );
        doAnswer(inv -> {
            config.setHooks(List.of("H1"));
            return null;
        })
            .when(jdbcTemplate)
            .query(contains("portal_notification_config_hooks"), any(RowCallbackHandler.class));

        doAnswer(inv -> {
            config.setGroups(Set.of("G1"));
            return null;
        })
            .when(jdbcTemplate)
            .query(contains("portal_notification_config_groups"), any(RowCallbackHandler.class));

        Set<PortalNotificationConfig> result = repository.findAll();
        PortalNotificationConfig cfg = result.iterator().next();

        assertThat(cfg.getHooks()).containsExactly("H1");
        assertThat(cfg.getGroups()).containsExactly("G1");
    }
}
