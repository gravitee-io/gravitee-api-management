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
import io.gravitee.gateway.services.sync.process.repository.synchronizer.authz.AuthzPolicyReactorDeployable;
import io.reactivex.rxjava3.core.Completable;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentLinkedQueue;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class AuthzPolicyDeployerTest {

    private RecordingPort port;
    private AuthzPolicyDeployer deployer;

    @BeforeEach
    void setUp() {
        port = new RecordingPort();
        deployer = new AuthzPolicyDeployer(
            port,
            new io.gravitee.gateway.services.sync.process.distributed.service.NoopDistributedSyncService()
        );
    }

    @Test
    void deploy_global_is_unconditional_broadcast() {
        AuthzPolicyReactorDeployable d = global("doc-1", "Default deny", "permit(p,a,r);");

        deployer.deploy(d).blockingAwait();

        assertThat(port.policyOps).hasSize(1);
        assertThat(port.policyOps.peek().op()).isEqualTo("addOrUpdatePolicy");
        assertThat(port.policyOps.peek().docId()).isEqualTo("doc-1");
        assertThat(port.policyOps.peek().policyText()).isEqualTo("permit(p,a,r);");
    }

    @Test
    void deploy_auto_derived_resource_policy_is_staged_unconditionally() {
        AuthzPolicyReactorDeployable d = resource("doc-2", "Bookings read", "api.bookings", "permit(...);");

        deployer.deploy(d).blockingAwait();

        assertThat(port.policyOps).hasSize(1);
        assertThat(port.policyOps.peek().docId()).isEqualTo("doc-2");
    }

    @Test
    void deploy_custom_resource_policy_broadcasts_unconditionally() {
        // RESOURCE policies referring to non-auto-derived entities must land on every
        // gateway node — the registry only partitions API-derived entities.
        AuthzPolicyReactorDeployable d = resource("doc-custom", "Custom doc read", "team.acme.docs", "permit(...);");

        deployer.deploy(d).blockingAwait();

        assertThat(port.policyOps).hasSize(1);
        assertThat(port.policyOps.peek().docId()).isEqualTo("doc-custom");
    }

    @Test
    void deploy_resource_with_null_entityId_is_staged() {
        AuthzPolicyReactorDeployable d = AuthzPolicyReactorDeployable.builder()
            .docId("doc-bad")
            .name("rogue")
            .kind(AuthzPolicyReactorDeployable.Kind.RESOURCE)
            .entityId(null)
            .policyText("permit(...);")
            .syncAction(SyncAction.DEPLOY)
            .build();

        deployer.deploy(d).blockingAwait();

        assertThat(port.policyOps).hasSize(1);
        assertThat(port.policyOps.peek().docId()).isEqualTo("doc-bad");
    }

    @Test
    void undeploy_does_not_filter_by_registry_or_kind() {
        AuthzPolicyReactorDeployable d = AuthzPolicyReactorDeployable.builder()
            .docId("doc-4")
            .name("x")
            .kind(AuthzPolicyReactorDeployable.Kind.RESOURCE)
            .entityId("api.unregistered")
            .syncAction(SyncAction.UNDEPLOY)
            .build();

        deployer.undeploy(d).blockingAwait();

        assertThat(port.policyOps).hasSize(1);
        assertThat(port.policyOps.peek().op()).isEqualTo("removePolicy");
    }

    @Test
    void undeploy_contract_pinned_kind_and_entityId_are_ignored() {
        AuthzPolicyReactorDeployable globalUndeploy = AuthzPolicyReactorDeployable.builder()
            .docId("doc-x")
            .name("x")
            .kind(AuthzPolicyReactorDeployable.Kind.GLOBAL)
            .entityId(null)
            .syncAction(SyncAction.UNDEPLOY)
            .build();
        AuthzPolicyReactorDeployable resourceUndeployUnregistered = AuthzPolicyReactorDeployable.builder()
            .docId("doc-y")
            .name("y")
            .kind(AuthzPolicyReactorDeployable.Kind.RESOURCE)
            .entityId("api.never-deployed-on-this-node")
            .syncAction(SyncAction.UNDEPLOY)
            .build();

        deployer.undeploy(globalUndeploy).blockingAwait();
        deployer.undeploy(resourceUndeployUnregistered).blockingAwait();

        assertThat(port.policyOps).hasSize(2);
        assertThat(port.policyOps).allMatch(op -> op.op().equals("removePolicy"));
    }

    @Test
    void deploy_records_scope_ids_on_the_op() {
        AuthzPolicyReactorDeployable d = AuthzPolicyReactorDeployable.builder()
            .docId("doc-scoped")
            .name("scoped")
            .kind(AuthzPolicyReactorDeployable.Kind.RESOURCE)
            .entityId("api.a")
            .policyText("permit(p,a,r);")
            .targetPdpIds(Set.of("api-a"))
            .syncAction(SyncAction.DEPLOY)
            .build();

        deployer.deploy(d).blockingAwait();

        assertThat(port.policyOps).hasSize(1);
        assertThat(port.policyOps.peek().targetPdpIds()).isEqualTo(Set.of("api-a"));
    }

    @Test
    void deploy_evicts_dropped_scopes_before_re_staging_targets() {
        AuthzPolicyReactorDeployable d = AuthzPolicyReactorDeployable.builder()
            .docId("doc-rt")
            .name("retargeted")
            .kind(AuthzPolicyReactorDeployable.Kind.GLOBAL)
            .policyText("permit(p,a,r);")
            .targetPdpIds(Set.of("scope-b"))
            .removedTargetPdpIds(Set.of("scope-a"))
            .syncAction(SyncAction.DEPLOY)
            .build();

        deployer.deploy(d).blockingAwait();

        List<RecordingPort.PolicyOp> ops = new java.util.ArrayList<>(port.policyOps);
        assertThat(ops).hasSize(2);
        assertThat(ops.get(0).op()).isEqualTo("removePolicy");
        assertThat(ops.get(0).targetPdpIds()).isEqualTo(Set.of("scope-a"));
        assertThat(ops.get(1).op()).isEqualTo("addOrUpdatePolicy");
        assertThat(ops.get(1).targetPdpIds()).isEqualTo(Set.of("scope-b"));
    }

    @Test
    void deploy_with_no_dropped_scopes_skips_removal() {
        AuthzPolicyReactorDeployable d = global("doc-clean", "clean", "permit(p,a,r);");

        deployer.deploy(d).blockingAwait();

        assertThat(port.policyOps).hasSize(1);
        assertThat(port.policyOps.peek().op()).isEqualTo("addOrUpdatePolicy");
    }

    private static AuthzPolicyReactorDeployable global(String docId, String name, String text) {
        return AuthzPolicyReactorDeployable.builder()
            .docId(docId)
            .name(name)
            .kind(AuthzPolicyReactorDeployable.Kind.GLOBAL)
            .policyText(text)
            .syncAction(SyncAction.DEPLOY)
            .build();
    }

    private static AuthzPolicyReactorDeployable resource(String docId, String name, String entityId, String text) {
        return AuthzPolicyReactorDeployable.builder()
            .docId(docId)
            .name(name)
            .kind(AuthzPolicyReactorDeployable.Kind.RESOURCE)
            .entityId(entityId)
            .policyText(text)
            .syncAction(SyncAction.DEPLOY)
            .build();
    }

    private static class RecordingPort implements AuthzEnginePort {

        record PolicyOp(String op, String docId, String name, String policyText, Set<String> targetPdpIds) {}

        final ConcurrentLinkedQueue<PolicyOp> policyOps = new ConcurrentLinkedQueue<>();

        @Override
        public Completable addOrUpdateEntity(
            String environmentId,
            String uid,
            Map<String, Object> attributes,
            List<String> parents,
            Set<String> targetPdpIds
        ) {
            return Completable.complete();
        }

        @Override
        public Completable removeEntity(String environmentId, String uid, Set<String> targetPdpIds) {
            return Completable.complete();
        }

        @Override
        public Completable addOrUpdatePolicy(String environmentId, String docId, String name, String policyText, Set<String> targetPdpIds) {
            policyOps.add(new PolicyOp("addOrUpdatePolicy", docId, name, policyText, targetPdpIds));
            return Completable.complete();
        }

        @Override
        public Completable removePolicy(String environmentId, String docId, Set<String> targetPdpIds) {
            policyOps.add(new PolicyOp("removePolicy", docId, null, null, targetPdpIds));
            return Completable.complete();
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
