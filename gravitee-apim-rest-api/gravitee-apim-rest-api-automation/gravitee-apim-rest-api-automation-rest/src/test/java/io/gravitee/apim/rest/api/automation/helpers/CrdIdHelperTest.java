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
package io.gravitee.apim.rest.api.automation.helpers;

import static org.assertj.core.api.Assertions.assertThat;

import io.gravitee.apim.core.audit.model.AuditInfo;
import io.gravitee.apim.core.group.model.crd.GroupCRDSpec;
import io.gravitee.rest.api.service.common.HRIDToUUID;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class CrdIdHelperTest {

    private static final String ORGANIZATION = "org-id";
    private static final String ENVIRONMENT = "env-id";
    private static final AuditInfo AUDIT_INFO = AuditInfo.builder().organizationId(ORGANIZATION).environmentId(ENVIRONMENT).build();

    @Nested
    class GenerateGroupId {

        @Test
        void should_generate_id_from_name() {
            var spec = GroupCRDSpec.builder().name("my-group").build();

            CrdIdHelper.generateGroupId(spec, AUDIT_INFO);

            assertThat(spec.getId()).isNotNull().isEqualTo(HRIDToUUID.group().context(AUDIT_INFO).hrid("my-group").id());
        }

        @Test
        void should_not_override_existing_id() {
            var existingId = "existing-uuid";
            var spec = GroupCRDSpec.builder().id(existingId).name("my-group").build();

            CrdIdHelper.generateGroupId(spec, AUDIT_INFO);

            assertThat(spec.getId()).isEqualTo(existingId);
        }

        @Test
        void should_generate_deterministic_id() {
            var spec1 = GroupCRDSpec.builder().name("my-group").build();
            var spec2 = GroupCRDSpec.builder().name("my-group").build();

            CrdIdHelper.generateGroupId(spec1, AUDIT_INFO);
            CrdIdHelper.generateGroupId(spec2, AUDIT_INFO);

            assertThat(spec1.getId()).isEqualTo(spec2.getId());
        }

        @Test
        void should_generate_different_ids_for_different_names() {
            var spec1 = GroupCRDSpec.builder().name("group-a").build();
            var spec2 = GroupCRDSpec.builder().name("group-b").build();

            CrdIdHelper.generateGroupId(spec1, AUDIT_INFO);
            CrdIdHelper.generateGroupId(spec2, AUDIT_INFO);

            assertThat(spec1.getId()).isNotEqualTo(spec2.getId());
        }

        @Test
        void should_generate_different_ids_for_different_environments() {
            var audit1 = AuditInfo.builder().organizationId(ORGANIZATION).environmentId("env-1").build();
            var audit2 = AuditInfo.builder().organizationId(ORGANIZATION).environmentId("env-2").build();

            var spec1 = GroupCRDSpec.builder().name("my-group").build();
            var spec2 = GroupCRDSpec.builder().name("my-group").build();

            CrdIdHelper.generateGroupId(spec1, audit1);
            CrdIdHelper.generateGroupId(spec2, audit2);

            assertThat(spec1.getId()).isNotEqualTo(spec2.getId());
        }

        @Test
        void should_generate_different_ids_for_different_organizations() {
            var audit1 = AuditInfo.builder().organizationId("org-1").environmentId(ENVIRONMENT).build();
            var audit2 = AuditInfo.builder().organizationId("org-2").environmentId(ENVIRONMENT).build();

            var spec1 = GroupCRDSpec.builder().name("my-group").build();
            var spec2 = GroupCRDSpec.builder().name("my-group").build();

            CrdIdHelper.generateGroupId(spec1, audit1);
            CrdIdHelper.generateGroupId(spec2, audit2);

            assertThat(spec1.getId()).isNotEqualTo(spec2.getId());
        }
    }
}
