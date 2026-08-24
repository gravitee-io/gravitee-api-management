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
import java.util.Date;
import org.junit.jupiter.api.Test;

class AuthzSchemaMapperTest {

    private final AuthzSchemaMapper mapper = new AuthzSchemaMapper(new ObjectMapper());

    @Test
    void toDeploy_yields_a_deployable_carrying_the_schema_source_and_targets() {
        Event event = event(
            "evt-1",
            """
            {
              "id": "doc-uuid-1",
              "name": "Fleet schema",
              "schemaText": "namespace ds { entity Car; }",
              "environmentId": "env-1",
              "targetPdpIds": ["stock@eu", "orders"]
            }
            """
        );
        event.setUpdatedAt(new Date(1_700_000_000_000L));

        AuthzSchemaReactorDeployable d = mapper.toDeploy(event).blockingGet();

        assertThat(d).isNotNull();
        assertThat(d.docId()).isEqualTo("doc-uuid-1");
        assertThat(d.name()).isEqualTo("Fleet schema");
        assertThat(d.schemaText()).isEqualTo("namespace ds { entity Car; }");
        assertThat(d.environmentId()).isEqualTo("env-1");
        assertThat(d.targetPdpIds()).containsExactlyInAnyOrder("stock@eu", "orders");
        assertThat(d.updatedAt()).isEqualTo(1_700_000_000_000L);
        assertThat(d.syncAction()).isEqualTo(SyncAction.DEPLOY);
    }

    @Test
    void toDeploy_falls_back_to_the_id_when_the_name_is_missing() {
        AuthzSchemaReactorDeployable d = mapper
            .toDeploy(
                event(
                    "evt-2",
                    """
                    {"id": "doc-uuid-2", "schemaText": "entity User;"}
                    """
                )
            )
            .blockingGet();

        assertThat(d.name()).isEqualTo("doc-uuid-2");
    }

    @Test
    void toDeploy_skips_an_event_without_an_id() {
        AuthzSchemaReactorDeployable d = mapper
            .toDeploy(
                event(
                    "evt-3",
                    """
                    {"schemaText": "entity User;"}
                    """
                )
            )
            .blockingGet();

        assertThat(d).isNull();
    }

    @Test
    void toDeploy_skips_an_event_whose_schemaText_is_blank() {
        AuthzSchemaReactorDeployable d = mapper
            .toDeploy(
                event(
                    "evt-4",
                    """
                    {"id": "doc-uuid-4", "schemaText": "   "}
                    """
                )
            )
            .blockingGet();

        assertThat(d).as("a blank document would flip the engine out of the no-schema state for nothing").isNull();
    }

    @Test
    void toDeploy_skips_an_unparseable_payload_instead_of_failing_the_cycle() {
        AuthzSchemaReactorDeployable d = mapper.toDeploy(event("evt-5", "{not json")).blockingGet();

        assertThat(d).isNull();
    }

    @Test
    void toUndeploy_needs_only_the_id_and_the_targets() {
        AuthzSchemaReactorDeployable d = mapper
            .toUndeploy(
                event(
                    "evt-6",
                    """
                    {"id": "doc-uuid-6", "environmentId": "env-1", "targetPdpIds": ["orders"]}
                    """
                )
            )
            .blockingGet();

        assertThat(d).isNotNull();
        assertThat(d.docId()).isEqualTo("doc-uuid-6");
        assertThat(d.targetPdpIds()).containsExactly("orders");
        assertThat(d.syncAction()).isEqualTo(SyncAction.UNDEPLOY);
    }

    @Test
    void toUndeploy_does_not_require_schemaText() {
        AuthzSchemaReactorDeployable d = mapper
            .toUndeploy(
                event(
                    "evt-7",
                    """
                    {"id": "doc-uuid-7"}
                    """
                )
            )
            .blockingGet();

        assertThat(d).isNotNull();
        assertThat(d.docId()).isEqualTo("doc-uuid-7");
    }

    @Test
    void toUndeploy_skips_an_event_without_an_id() {
        AuthzSchemaReactorDeployable d = mapper
            .toUndeploy(
                event(
                    "evt-8",
                    """
                    {"name": "orphan"}
                    """
                )
            )
            .blockingGet();

        assertThat(d).isNull();
    }

    @Test
    void an_absent_target_set_maps_to_empty_not_null() {
        AuthzSchemaReactorDeployable d = mapper
            .toDeploy(
                event(
                    "evt-9",
                    """
                    {"id": "doc-uuid-9", "schemaText": "entity User;"}
                    """
                )
            )
            .blockingGet();

        assertThat(d.targetPdpIds()).isNotNull().isEmpty();
    }

    private static Event event(String id, String payload) {
        Event event = new Event();
        event.setId(id);
        event.setPayload(payload);
        return event;
    }
}
