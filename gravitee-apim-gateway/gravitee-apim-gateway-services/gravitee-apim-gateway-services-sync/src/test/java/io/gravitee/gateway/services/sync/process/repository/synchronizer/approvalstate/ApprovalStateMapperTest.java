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
package io.gravitee.gateway.services.sync.process.repository.synchronizer.approvalstate;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import io.gravitee.gamma.definition.approval.ApprovalCaller;
import io.gravitee.gamma.definition.approval.ApprovalState;
import io.gravitee.gateway.handlers.api.manager.DeployedApprovalState;
import io.gravitee.gateway.handlers.api.manager.DeployedApprovalState.ExecutorLease;
import io.gravitee.gateway.handlers.api.manager.DeployedApprovalState.Grant;
import io.gravitee.gateway.services.sync.process.common.model.SyncAction;
import io.gravitee.repository.management.model.Event;
import io.gravitee.repository.management.model.EventType;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
class ApprovalStateMapperTest {

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final ApprovalStateMapper mapper = new ApprovalStateMapper(objectMapper);

    @Nested
    class ToDeploy {

        @Test
        void should_read_the_state_with_its_timestamps_in_epoch_milliseconds() throws Exception {
            ApprovalStateDeployable deployable = mapper.toDeploy(publish(objectMapper.writeValueAsString(wire()))).blockingGet();

            assertThat(deployable.id()).isEqualTo("req-1");
            assertThat(deployable.syncAction()).isEqualTo(SyncAction.DEPLOY);
            assertThat(deployable.state()).isEqualTo(
                new DeployedApprovalState(
                    "req-1",
                    List.of("alias-1"),
                    "env-1",
                    "api-1",
                    "rule-1",
                    "2026-10-01T00:00:00Z",
                    "open-1",
                    "APPROVED",
                    millis("2026-10-31T11:00:00Z"),
                    null,
                    new Grant("TIMED", millis("2026-10-31T12:00:00Z"), "app-1", null, "digest-1"),
                    new ExecutorLease("node-1", "lease-1", millis("2026-10-31T10:01:00Z")),
                    millis("2026-10-31T13:00:00Z"),
                    millis("2026-10-31T09:00:00Z"),
                    millis("2026-10-31T09:30:00Z"),
                    millis("2026-10-31T10:00:00Z"),
                    7L
                )
            );
        }

        @Test
        void should_read_a_pending_state_without_grant_nor_executor() throws Exception {
            ApprovalState wire = wire();
            wire.setStatus("PENDING");
            wire.setGrant(null);
            wire.setExecutor(null);
            wire.setExpiresAt(null);
            wire.setAliasIds(null);
            wire.setStartedAt(null);

            DeployedApprovalState state = mapper.toDeploy(publish(objectMapper.writeValueAsString(wire))).blockingGet().state();

            assertThat(state.grant()).isNull();
            assertThat(state.executor()).isNull();
            assertThat(state.expiresAt()).isNull();
            assertThat(state.aliasIds()).isEmpty();
            assertThat(state.startedAt()).isNull();
        }

        @Test
        void should_read_a_state_without_version_from_an_older_control_plane_as_version_zero() throws Exception {
            ObjectNode payload = objectMapper.valueToTree(wire());
            payload.remove("version");

            assertThat(mapper.toDeploy(publish(payload.toString())).blockingGet().state().version()).isZero();
        }

        @Test
        void should_read_a_released_request_without_open_key() throws Exception {
            ApprovalState wire = wire();
            wire.setOpenKey(null);

            assertThat(mapper.toDeploy(publish(objectMapper.writeValueAsString(wire))).blockingGet().state().openKey()).isNull();
        }

        @Test
        void should_read_a_once_grant_without_end() throws Exception {
            ApprovalState wire = wire();
            wire.getGrant().setScope("ONCE");
            wire.getGrant().setCoversUntil(null);

            DeployedApprovalState state = mapper.toDeploy(publish(objectMapper.writeValueAsString(wire))).blockingGet().state();

            assertThat(state.grant().coversUntil()).isNull();
        }

        @Test
        void should_ignore_fields_added_by_a_newer_control_plane() throws Exception {
            ObjectNode payload = objectMapper.valueToTree(wire());
            payload.put("addedLater", "value");

            assertThat(mapper.toDeploy(publish(payload.toString())).blockingGet()).isNotNull();
        }

        @ParameterizedTest
        @ValueSource(strings = { "approvalRequestId", "environmentId", "apiId", "ruleId", "status", "keepUntil", "createdAt", "updatedAt" })
        void should_skip_a_state_without_a_required_field(String field) {
            ObjectNode payload = objectMapper.valueToTree(wire());
            payload.remove(field);

            assertThat(mapper.toDeploy(publish(payload.toString())).blockingGet()).isNull();
        }

        @ParameterizedTest
        @ValueSource(strings = { "keepUntil", "createdAt", "startedAt", "updatedAt", "expiresAt" })
        void should_skip_a_state_with_an_unreadable_timestamp(String field) {
            ObjectNode payload = objectMapper.valueToTree(wire());
            payload.put(field, "yesterday");

            assertThat(mapper.toDeploy(publish(payload.toString())).blockingGet()).isNull();
        }

        @Test
        void should_skip_a_time_based_grant_without_end() {
            assertSkipped(wire -> wire.getGrant().setCoversUntil(null));
        }

        @Test
        void should_skip_a_grant_without_caller_or_digest() {
            assertSkipped(wire -> wire.getGrant().setCaller(null));
            assertSkipped(wire -> wire.getGrant().getCaller().setApplicationId(null));
            assertSkipped(wire -> wire.getGrant().setArgumentsDigest(null));
            assertSkipped(wire -> wire.getGrant().setScope(null));
        }

        @Test
        void should_skip_an_executor_lease_without_node_or_end() {
            assertSkipped(wire -> wire.getExecutor().setNodeId(null));
            assertSkipped(wire -> wire.getExecutor().setLeaseId(null));
            assertSkipped(wire -> wire.getExecutor().setLeaseUntil("soon"));
        }

        @Test
        void should_skip_an_unreadable_payload() {
            assertThat(mapper.toDeploy(publish("not-json")).blockingGet()).isNull();
        }

        private void assertSkipped(Consumer<ApprovalState> change) {
            ApprovalState wire = wire();
            change.accept(wire);
            try {
                assertThat(mapper.toDeploy(publish(objectMapper.writeValueAsString(wire))).blockingGet()).isNull();
            } catch (Exception e) {
                throw new AssertionError(e);
            }
        }
    }

    @Nested
    class ToUndeploy {

        @Test
        void should_read_the_id_from_the_payload() {
            ApprovalStateDeployable deployable = mapper
                .toUndeploy(event(EventType.UNPUBLISH_APPROVAL_STATE, "{\"approvalRequestId\":\"req-1\"}", Map.of()))
                .blockingGet();

            assertThat(deployable.id()).isEqualTo("req-1");
            assertThat(deployable.syncAction()).isEqualTo(SyncAction.UNDEPLOY);
            assertThat(deployable.state()).isNull();
        }

        @Test
        void should_read_the_id_from_the_event_key_when_the_payload_has_none() {
            ApprovalStateDeployable deployable = mapper
                .toUndeploy(
                    event(EventType.UNPUBLISH_APPROVAL_STATE, null, Map.of(Event.EventProperties.APPROVAL_REQUEST_ID.getValue(), "req-1"))
                )
                .blockingGet();

            assertThat(deployable.id()).isEqualTo("req-1");
        }

        @Test
        void should_skip_an_event_without_id() {
            assertThat(mapper.toUndeploy(event(EventType.UNPUBLISH_APPROVAL_STATE, "{}", Map.of())).blockingGet()).isNull();
        }
    }

    private static ApprovalState wire() {
        return ApprovalState.builder()
            .approvalRequestId("req-1")
            .aliasIds(List.of("alias-1"))
            .environmentId("env-1")
            .apiId("api-1")
            .ruleId("rule-1")
            .ruleUpdatedAt("2026-10-01T00:00:00Z")
            .openKey("open-1")
            .status("APPROVED")
            .expiresAt("2026-10-31T11:00:00Z")
            .grant(new ApprovalState.Grant("TIMED", "2026-10-31T12:00:00Z", new ApprovalCaller("app-1", null), "digest-1"))
            .executor(new ApprovalState.ExecutorLease("node-1", "lease-1", "2026-10-31T10:01:00Z"))
            .keepUntil("2026-10-31T13:00:00Z")
            .createdAt("2026-10-31T09:00:00Z")
            .startedAt("2026-10-31T09:30:00Z")
            .updatedAt("2026-10-31T10:00:00Z")
            .version(7L)
            .build();
    }

    private static long millis(String instant) {
        return Instant.parse(instant).toEpochMilli();
    }

    private static Event publish(String payload) {
        return event(EventType.PUBLISH_APPROVAL_STATE, payload, Map.of());
    }

    private static Event event(EventType type, String payload, Map<String, String> properties) {
        Event event = new Event();
        event.setId("evt-1");
        event.setType(type);
        event.setPayload(payload);
        event.setProperties(properties);
        return event;
    }
}
