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
package io.gravitee.gateway.services.sync.process.repository.synchronizer.authz;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.gravitee.gateway.services.sync.process.common.model.SyncAction;
import io.gravitee.repository.management.model.Event;
import org.junit.jupiter.api.Test;

class AuthzPolicyMapperTest {

    private final AuthzPolicyMapper mapper = new AuthzPolicyMapper(new ObjectMapper());

    @Test
    void toDeploy_global_policy_yields_deployable_without_entityId() {
        Event event = event(
            "evt-1",
            """
            {
              "id": "doc-uuid-1",
              "name": "Global default-deny",
              "kind": "GLOBAL",
              "entityId": null,
              "policyText": "permit(principal, action, resource);",
              "environmentId": "env-1"
            }
            """
        );

        AuthzPolicyReactorDeployable d = mapper.toDeploy(event).blockingGet();

        assertThat(d).isNotNull();
        assertThat(d.docId()).isEqualTo("doc-uuid-1");
        assertThat(d.name()).isEqualTo("Global default-deny");
        assertThat(d.kind()).isEqualTo(AuthzPolicyReactorDeployable.Kind.GLOBAL);
        assertThat(d.entityId()).isNull();
        assertThat(d.policyText()).isEqualTo("permit(principal, action, resource);");
        assertThat(d.syncAction()).isEqualTo(SyncAction.DEPLOY);
    }

    @Test
    void toDeploy_resource_policy_carries_entityId_for_registry_filter() {
        Event event = event(
            "evt-2",
            """
            {
              "id": "doc-uuid-2",
              "name": "Booking read",
              "kind": "RESOURCE",
              "entityId": "api.bookings",
              "policyText": "permit(principal, action == Action::\\"read\\", resource);"
            }
            """
        );

        AuthzPolicyReactorDeployable d = mapper.toDeploy(event).blockingGet();

        assertThat(d).isNotNull();
        assertThat(d.kind()).isEqualTo(AuthzPolicyReactorDeployable.Kind.RESOURCE);
        assertThat(d.entityId()).isEqualTo("api.bookings");
    }

    @Test
    void toDeploy_resource_policy_without_entityId_returns_empty() {
        Event event = event("evt-3", "{\"id\": \"doc-3\", \"name\": \"r\", \"kind\": \"RESOURCE\", \"policyText\": \"permit(...);\"}");

        assertThat(mapper.toDeploy(event).blockingGet()).isNull();
    }

    @Test
    void toDeploy_missing_id_returns_empty() {
        Event event = event("evt-4", "{\"kind\": \"GLOBAL\", \"policyText\": \"x\"}");

        assertThat(mapper.toDeploy(event).blockingGet()).isNull();
    }

    @Test
    void toDeploy_missing_policyText_returns_empty() {
        Event event = event("evt-5", "{\"id\": \"doc\", \"name\": \"n\", \"kind\": \"GLOBAL\"}");

        assertThat(mapper.toDeploy(event).blockingGet()).isNull();
    }

    @Test
    void toDeploy_whitespace_only_policyText_returns_empty() {
        Event event = event("evt-blank", "{\"id\": \"doc-blank\", \"name\": \"n\", \"kind\": \"GLOBAL\", \"policyText\": \"   \"}");

        assertThat(mapper.toDeploy(event).blockingGet()).isNull();
    }

    @Test
    void toDeploy_unknown_kind_returns_empty() {
        Event event = event("evt-6", "{\"id\": \"doc\", \"name\": \"n\", \"kind\": \"UNICORN\", \"policyText\": \"x\"}");

        assertThat(mapper.toDeploy(event).blockingGet()).isNull();
    }

    @Test
    void toDeploy_unparseable_json_returns_empty() {
        Event event = event("evt-7", "this is not json");

        assertThat(mapper.toDeploy(event).blockingGet()).isNull();
    }

    @Test
    void toDeploy_blank_name_falls_back_to_docId_for_diagnostics_stability() {
        Event event = event("evt-8", "{\"id\": \"doc-8\", \"name\": \"\", \"kind\": \"GLOBAL\", \"policyText\": \"permit(p,a,r);\"}");

        AuthzPolicyReactorDeployable d = mapper.toDeploy(event).blockingGet();

        assertThat(d).isNotNull();
        assertThat(d.name()).isEqualTo("doc-8");
    }

    @Test
    void toUndeploy_minimal_payload_yields_undeploy_with_GLOBAL_default() {
        Event event = event("evt-9", "{\"id\": \"doc-9\", \"environmentId\": \"env-1\"}");

        AuthzPolicyReactorDeployable d = mapper.toUndeploy(event).blockingGet();

        assertThat(d).isNotNull();
        assertThat(d.docId()).isEqualTo("doc-9");
        assertThat(d.kind()).isEqualTo(AuthzPolicyReactorDeployable.Kind.GLOBAL);
        assertThat(d.syncAction()).isEqualTo(SyncAction.UNDEPLOY);
    }

    @Test
    void toUndeploy_missing_id_returns_empty() {
        Event event = event("evt-10", "{\"environmentId\": \"env-1\"}");

        assertThat(mapper.toUndeploy(event).blockingGet()).isNull();
    }

    private static Event event(String id, String payload) {
        Event event = new Event();
        event.setId(id);
        event.setPayload(payload);
        return event;
    }
}
