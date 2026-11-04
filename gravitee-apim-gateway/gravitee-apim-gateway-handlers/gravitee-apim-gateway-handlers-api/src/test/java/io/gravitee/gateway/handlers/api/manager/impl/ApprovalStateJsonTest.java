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
package io.gravitee.gateway.handlers.api.manager.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.gravitee.gateway.handlers.api.manager.DeployedApprovalState;
import io.gravitee.gateway.handlers.api.manager.DeployedApprovalState.ExecutorLease;
import io.gravitee.gateway.handlers.api.manager.DeployedApprovalState.Grant;
import io.gravitee.gateway.handlers.api.manager.impl.ApprovalStateJson.Entry;
import java.util.List;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Test;

@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
class ApprovalStateJsonTest {

    private static final DeployedApprovalState STATE = new DeployedApprovalState(
        "req-1",
        List.of("alias-1"),
        "env-1",
        "api-1",
        "rule-1",
        "2026-10-01T00:00:00Z",
        "open-1",
        "APPROVED",
        7_000L,
        "a reason",
        new Grant("TIMED", 8_000L, "app-1", "sub-1", "digest-1"),
        new ExecutorLease("node-1", "lease-1", 6_000L),
        9_000L,
        500L,
        800L,
        1_000L,
        7L
    );

    // Fails on unknown properties, as Jackson does by default: the wire records must not rely on a lenient mapper.
    private final ApprovalStateJson cut = new ApprovalStateJson(new ObjectMapper());

    @Test
    void should_read_back_the_state_and_organization_it_wrote() throws JsonProcessingException {
        assertThat(cut.read(cut.write("org-1", STATE))).isEqualTo(new Entry("org-1", STATE));
    }

    @Test
    void should_read_a_payload_written_by_a_node_that_knows_more_properties() throws JsonProcessingException {
        String payload = """
            {"organizationId": "org-1", "addedLater": true,
             "state": {"id": "req-1", "aliasIds": ["alias-1"], "environmentId": "env-1", "apiId": "api-1", "ruleId": "rule-1",
               "ruleUpdatedAt": "2026-10-01T00:00:00Z", "openKey": "open-1", "status": "APPROVED", "expiresAt": 7000,
               "reason": "a reason", "keepUntil": 9000, "createdAt": 500, "startedAt": 800, "updatedAt": 1000, "version": 7, "addedLater": "x",
               "grant": {"scope": "TIMED", "coversUntil": 8000, "applicationId": "app-1", "subject": "sub-1",
                         "argumentsDigest": "digest-1", "addedLater": 1},
               "executor": {"nodeId": "node-1", "leaseId": "lease-1", "leaseUntil": 6000, "addedLater": []}}}
            """;

        assertThat(cut.read(payload)).isEqualTo(new Entry("org-1", STATE));
    }

    @Test
    void should_read_a_payload_written_by_a_node_that_knows_fewer_properties() throws JsonProcessingException {
        String payload = """
            {"organizationId": "org-1",
             "state": {"id": "req-1", "environmentId": "env-1", "apiId": "api-1", "ruleId": "rule-1", "status": "PENDING",
               "keepUntil": 9000, "createdAt": 500, "updatedAt": 1000}}
            """;

        DeployedApprovalState state = cut.read(payload).state();

        assertThat(state.aliasIds()).isEmpty();
        assertThat(state.grant()).isNull();
        assertThat(state.executor()).isNull();
        assertThat(state.version()).isZero();
        assertThat(state.startedAt()).isNull();
    }

    @Test
    void should_refuse_a_payload_without_organization_or_state() throws JsonProcessingException {
        String withoutOrganization = cut.write(null, STATE);

        assertThatThrownBy(() -> cut.read(withoutOrganization)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> cut.read("{\"organizationId\": \"org-1\"}")).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void should_refuse_a_payload_that_is_not_json() {
        assertThatThrownBy(() -> cut.read("wrong")).isInstanceOf(JsonProcessingException.class);
    }
}
