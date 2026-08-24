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
package io.gravitee.gateway.services.sync.process.common.deployer;

import static org.assertj.core.api.Assertions.assertThat;

import io.gravitee.gateway.services.sync.process.common.model.SyncAction;
import io.gravitee.gateway.services.sync.process.repository.synchronizer.authz.AuthzEnginePort;
import io.gravitee.gateway.services.sync.process.repository.synchronizer.authz.AuthzSchemaReactorDeployable;
import io.reactivex.rxjava3.core.Completable;
import io.reactivex.rxjava3.subjects.CompletableSubject;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentLinkedQueue;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class AuthzSchemaDeployerTest {

    private RecordingPort port;
    private AuthzSchemaDeployer deployer;

    @BeforeEach
    void setUp() {
        port = new RecordingPort();
        deployer = new AuthzSchemaDeployer(port);
    }

    @Test
    void deploy_stages_the_document_against_its_targets() {
        deployer.deploy(schema("doc-1", "Fleet schema", "entity User;", Set.of("orders"), null)).blockingAwait();

        assertThat(port.ops).hasSize(1);
        RecordingPort.SchemaOp op = port.ops.peek();
        assertThat(op.op()).isEqualTo("addOrUpdateSchema");
        assertThat(op.docId()).isEqualTo("doc-1");
        assertThat(op.name()).isEqualTo("Fleet schema");
        assertThat(op.schemaText()).isEqualTo("entity User;");
        assertThat(op.targetPdpIds()).containsExactly("orders");
    }

    @Test
    void deploy_does_not_start_staging_until_the_eviction_has_completed() {
        // Ordering, proven by holding the eviction open. Asserting only the recorded sequence is not enough:
        // with both sources synchronous, even a concurrent operator like mergeWith produces remove-then-add,
        // so such a test stays green after the sequencing is broken.
        port.removeGate = CompletableSubject.create();

        deployer.deploy(schema("doc-2", "n", "entity User;", Set.of("orders"), Set.of("stock"))).subscribe();

        assertThat(port.ops)
            .extracting(RecordingPort.SchemaOp::op)
            .as("staging must not begin while the eviction is still in flight")
            .containsExactly("removeSchema");

        port.removeGate.onComplete();

        assertThat(port.ops).extracting(RecordingPort.SchemaOp::op).containsExactly("removeSchema", "addOrUpdateSchema");
    }

    @Test
    void deploy_evicts_dropped_scopes_before_staging_the_new_targets() {
        deployer.deploy(schema("doc-2", "n", "entity User;", Set.of("orders"), Set.of("stock"))).blockingAwait();

        assertThat(port.ops).hasSize(2);
        assertThat(port.ops)
            .extracting(RecordingPort.SchemaOp::op)
            .as("eviction must precede staging, otherwise a retarget leaves the schema live on the old engine")
            .containsExactly("removeSchema", "addOrUpdateSchema");
        assertThat(port.ops.peek().targetPdpIds()).containsExactly("stock");
    }

    @Test
    void deploy_without_dropped_scopes_does_not_emit_a_removal() {
        deployer.deploy(schema("doc-3", "n", "entity User;", Set.of("orders"), Set.of())).blockingAwait();

        assertThat(port.ops).extracting(RecordingPort.SchemaOp::op).containsExactly("addOrUpdateSchema");
    }

    @Test
    void undeploy_removes_the_document_from_its_targets() {
        AuthzSchemaReactorDeployable d = schema("doc-4", "n", "entity User;", Set.of("orders", "stock"), null);
        d.syncAction(SyncAction.UNDEPLOY);

        deployer.undeploy(d).blockingAwait();

        assertThat(port.ops).hasSize(1);
        assertThat(port.ops.peek().op()).isEqualTo("removeSchema");
        assertThat(port.ops.peek().docId()).isEqualTo("doc-4");
        assertThat(port.ops.peek().targetPdpIds()).containsExactlyInAnyOrder("orders", "stock");
    }

    @Test
    void the_deployer_never_touches_the_policy_or_entity_paths() {
        deployer.deploy(schema("doc-5", "n", "entity User;", Set.of("orders"), Set.of("stock"))).blockingAwait();

        assertThat(port.otherOps).isEmpty();
    }

    private static AuthzSchemaReactorDeployable schema(
        String docId,
        String name,
        String schemaText,
        Set<String> targets,
        Set<String> removed
    ) {
        return AuthzSchemaReactorDeployable.builder()
            .docId(docId)
            .name(name)
            .schemaText(schemaText)
            .environmentId("env-1")
            .targetPdpIds(targets)
            .removedTargetPdpIds(removed)
            .updatedAt(1L)
            .syncAction(SyncAction.DEPLOY)
            .build();
    }

    private static class RecordingPort implements AuthzEnginePort {

        record SchemaOp(String op, String docId, String name, String schemaText, Set<String> targetPdpIds) {}

        final ConcurrentLinkedQueue<SchemaOp> ops = new ConcurrentLinkedQueue<>();
        /** When set, removeSchema does not complete until the test releases it. */
        CompletableSubject removeGate;
        final ConcurrentLinkedQueue<String> otherOps = new ConcurrentLinkedQueue<>();

        @Override
        public Completable addOrUpdateEntity(
            String environmentId,
            String uid,
            Map<String, Object> attributes,
            List<String> parents,
            Set<String> targetPdpIds,
            long updatedAt
        ) {
            otherOps.add("addOrUpdateEntity");
            return Completable.complete();
        }

        @Override
        public Completable removeEntity(String environmentId, String uid, Set<String> targetPdpIds) {
            otherOps.add("removeEntity");
            return Completable.complete();
        }

        @Override
        public Completable addOrUpdatePolicy(
            String environmentId,
            String docId,
            String name,
            String policyText,
            Set<String> targetPdpIds,
            long updatedAt
        ) {
            otherOps.add("addOrUpdatePolicy");
            return Completable.complete();
        }

        @Override
        public Completable removePolicy(String environmentId, String docId, Set<String> targetPdpIds) {
            otherOps.add("removePolicy");
            return Completable.complete();
        }

        @Override
        public Completable addOrUpdateSchema(
            String environmentId,
            String docId,
            String name,
            String schemaText,
            Set<String> targetPdpIds,
            long updatedAt
        ) {
            // Recorded on subscribe, not when the chain is assembled: Java evaluates this call's arguments
            // before andThen() runs, so recording in the method body makes any ordering assertion vacuous.
            return Completable.fromAction(() -> ops.add(new SchemaOp("addOrUpdateSchema", docId, name, schemaText, targetPdpIds)));
        }

        @Override
        public Completable removeSchema(String environmentId, String docId, Set<String> targetPdpIds) {
            return Completable.fromAction(() -> ops.add(new SchemaOp("removeSchema", docId, null, null, targetPdpIds))).andThen(
                removeGate == null ? Completable.complete() : removeGate
            );
        }

        @Override
        public Completable commit() {
            return Completable.complete();
        }

        @Override
        public Completable commitScope(String environmentId, String targetPdpId) {
            return Completable.complete();
        }
    }
}
