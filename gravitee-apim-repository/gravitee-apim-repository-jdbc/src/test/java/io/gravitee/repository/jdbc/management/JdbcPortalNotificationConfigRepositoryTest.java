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

import io.gravitee.repository.management.api.search.PortalNotificationCriteria;
import io.gravitee.repository.management.model.NotificationReferenceType;
import org.junit.jupiter.api.Test;

public class JdbcPortalNotificationConfigRepositoryTest {

    private JdbcPortalNotificationConfigRepository repository = new JdbcPortalNotificationConfigRepository("table_prefix_");

    @Test
    public void generateQuery_withHookAndOrgId() {
        PortalNotificationCriteria criteria = PortalNotificationCriteria.builder().hook("hook1").organizationId("org1").build();
        String query = repository.generateQuery(criteria);
        String expectedQuery =
            "select pnc.`user`, pnc.reference_type, pnc.reference_id, pnc.created_at, pnc.updated_at, pnc.origin from " +
            "table_prefix_portal_notification_configs pnc left join table_prefix_portal_notification_config_hooks pnch on " +
            "pnc.reference_type = pnch.reference_type and pnc.reference_id = pnch.reference_id and pnc.`user` = pnch.`user` " +
            "where 1=1 and pnch.hook = ? and pnc.organization_id = ?";
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
            "select pnc.`user`, pnc.reference_type, pnc.reference_id, pnc.created_at, pnc.updated_at, pnc.origin from " +
            "table_prefix_portal_notification_configs pnc left join table_prefix_portal_notification_config_hooks pnch on " +
            "pnc.reference_type = pnch.reference_type and pnc.reference_id = pnch.reference_id and pnc.`user` = pnch.`user` " +
            "where 1=1 and pnc.reference_type = ? and pnc.reference_id = ? and pnch.hook = ?";
        assertThat(query).isEqualTo(expectedQuery);
    }
}
