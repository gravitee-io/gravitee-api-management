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

class AuthzEntityMapperTest {

    private final AuthzEntityMapper mapper = new AuthzEntityMapper(new ObjectMapper());

    @Test
    void toDeploy_resource_event_yields_deployable_with_engine_uid() {
        Event event = event(
            "evt-1",
            """
            {
              "id": "internal-uuid",
              "entityId": "custom.bookings",
              "kind": "RESOURCE",
              "attributes": {"region": "eu"},
              "parents": [],
              "source": "apim",
              "environmentId": "env-1",
              "updatedAt": "2026-01-01T00:00:00Z"
            }
            """
        );

        AuthzEntityReactorDeployable d = mapper.toDeploy(event).blockingGet();

        assertThat(d).isNotNull();
        assertThat(d.entityId()).isEqualTo("custom.bookings");
        assertThat(d.engineUid()).isEqualTo("Resource::\"custom.bookings\"");
        assertThat(d.kind()).isEqualTo(AuthzEntityReactorDeployable.Kind.RESOURCE);
        assertThat(d.attributes()).containsEntry("region", "eu");
        assertThat(d.parents()).isEmpty();
        assertThat(d.syncAction()).isEqualTo(SyncAction.DEPLOY);
    }

    @Test
    void toDeploy_auto_derived_entityId_is_passed_through_for_synchronizer_to_filter() {
        for (String autoDerived : new String[] { "api.bookings", "mcp.bookings.get", "agent.bot" }) {
            Event event = event(
                "evt-auto-" + autoDerived,
                String.format(
                    """
                    {
                      "entityId": "%s",
                      "kind": "RESOURCE",
                      "attributes": {},
                      "parents": []
                    }
                    """,
                    autoDerived
                )
            );

            AuthzEntityReactorDeployable d = mapper.toDeploy(event).blockingGet();
            assertThat(d).as("auto-derived id %s is decoded; filtering happens in the synchronizer", autoDerived).isNotNull();
            assertThat(d.entityId()).isEqualTo(autoDerived);

            AuthzEntityReactorDeployable u = mapper.toUndeploy(event).blockingGet();
            assertThat(u).as("auto-derived id %s undeploy is decoded; filtering happens in the synchronizer", autoDerived).isNotNull();
            assertThat(u.entityId()).isEqualTo(autoDerived);
        }
    }

    @Test
    void toDeploy_principal_event_yields_Principal_engine_uid() {
        Event event = event(
            "evt-2",
            """
            {
              "entityId": "idp.am.alice",
              "kind": "PRINCIPAL",
              "attributes": {"email": "alice@example.com"},
              "parents": ["idp.am.admins"]
            }
            """
        );

        AuthzEntityReactorDeployable d = mapper.toDeploy(event).blockingGet();

        assertThat(d).isNotNull();
        assertThat(d.engineUid()).isEqualTo("Principal::\"idp.am.alice\"");
        assertThat(d.kind()).isEqualTo(AuthzEntityReactorDeployable.Kind.PRINCIPAL);
        assertThat(d.parents()).containsExactly("idp.am.admins");
    }

    @Test
    void toDeploy_missing_entityId_returns_empty() {
        Event event = event("evt-3", "{\"kind\": \"RESOURCE\"}");

        assertThat(mapper.toDeploy(event).blockingGet()).isNull();
    }

    @Test
    void toDeploy_unknown_kind_returns_empty() {
        Event event = event("evt-4", "{\"entityId\": \"api.x\", \"kind\": \"GHOST\"}");

        assertThat(mapper.toDeploy(event).blockingGet()).isNull();
    }

    @Test
    void toDeploy_unparseable_json_returns_empty() {
        Event event = event("evt-5", "not-json");

        assertThat(mapper.toDeploy(event).blockingGet()).isNull();
    }

    @Test
    void toUndeploy_minimal_payload_yields_undeploy_deployable() {
        Event event = event("evt-6", "{\"entityId\": \"custom.bookings\", \"environmentId\": \"env-1\"}");

        AuthzEntityReactorDeployable d = mapper.toUndeploy(event).blockingGet();

        assertThat(d).isNotNull();
        assertThat(d.entityId()).isEqualTo("custom.bookings");
        assertThat(d.engineUid()).isEqualTo("Resource::\"custom.bookings\"");
        assertThat(d.syncAction()).isEqualTo(SyncAction.UNDEPLOY);
    }

    @Test
    void toUndeploy_missing_entityId_returns_empty() {
        Event event = event("evt-7", "{\"environmentId\": \"env-1\"}");

        assertThat(mapper.toUndeploy(event).blockingGet()).isNull();
    }

    @Test
    void toDeploy_typed_entityType_payload_propagates_to_deployable_and_engine_uid() {
        Event event = event(
            "evt-typed",
            """
            {
              "entityId": "alice",
              "kind": "PRINCIPAL",
              "entityType": "User",
              "attributes": {"email": "alice@example.com"},
              "parents": []
            }
            """
        );

        AuthzEntityReactorDeployable d = mapper.toDeploy(event).blockingGet();

        assertThat(d).isNotNull();
        assertThat(d.entityType()).isEqualTo("User");
        assertThat(d.engineUid()).isEqualTo("User::\"alice\"");
        assertThat(d.kind()).isEqualTo(AuthzEntityReactorDeployable.Kind.PRINCIPAL);
    }

    @Test
    void toUndeploy_typed_entityType_payload_propagates_to_deployable_and_engine_uid() {
        Event event = event(
            "evt-typed-undeploy",
            """
            {
              "entityId": "doc-1",
              "kind": "RESOURCE",
              "entityType": "Doc",
              "environmentId": "env-1"
            }
            """
        );

        AuthzEntityReactorDeployable d = mapper.toUndeploy(event).blockingGet();

        assertThat(d).isNotNull();
        assertThat(d.entityType()).isEqualTo("Doc");
        assertThat(d.engineUid()).isEqualTo("Doc::\"doc-1\"");
        assertThat(d.syncAction()).isEqualTo(SyncAction.UNDEPLOY);
    }

    @Test
    void toDeploy_legacy_payload_without_entityType_yields_null_entityType_on_deployable() {
        Event event = event(
            "evt-legacy",
            """
            {
              "entityId": "custom.bookings",
              "kind": "RESOURCE",
              "attributes": {},
              "parents": []
            }
            """
        );

        AuthzEntityReactorDeployable d = mapper.toDeploy(event).blockingGet();

        assertThat(d).isNotNull();
        assertThat(d.entityType()).isNull();
        assertThat(d.engineUid()).isEqualTo("Resource::\"custom.bookings\"");
    }

    @Test
    void toEngineUid_falls_back_to_kind_default_when_entityType_is_null_or_blank() {
        assertThat(AuthzEntityMapper.toEngineUid(AuthzEntityReactorDeployable.Kind.RESOURCE, null, "api.bookings")).isEqualTo(
            "Resource::\"api.bookings\""
        );
        assertThat(AuthzEntityMapper.toEngineUid(AuthzEntityReactorDeployable.Kind.PRINCIPAL, "", "idp.am.alice")).isEqualTo(
            "Principal::\"idp.am.alice\""
        );
    }

    @Test
    void toEngineUid_uses_explicit_entityType_when_present() {
        assertThat(AuthzEntityMapper.toEngineUid(AuthzEntityReactorDeployable.Kind.PRINCIPAL, "User", "alice")).isEqualTo(
            "User::\"alice\""
        );
        assertThat(AuthzEntityMapper.toEngineUid(AuthzEntityReactorDeployable.Kind.RESOURCE, "Doc", "doc-1")).isEqualTo("Doc::\"doc-1\"");
    }

    private static Event event(String id, String payload) {
        Event event = new Event();
        event.setId(id);
        event.setPayload(payload);
        return event;
    }
}
