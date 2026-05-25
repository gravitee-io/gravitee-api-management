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
import io.gravitee.gateway.services.sync.process.repository.service.AuthzRegistry;
import io.gravitee.gateway.services.sync.process.repository.synchronizer.authz.AuthzEnginePort;
import io.gravitee.gateway.services.sync.process.repository.synchronizer.authz.AuthzEntityReactorDeployable;
import io.reactivex.rxjava3.core.Completable;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentLinkedQueue;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class AuthzEntityDeployerTest {

    private AuthzRegistry registry;
    private RecordingPort port;
    private AuthzEntityDeployer deployer;

    @BeforeEach
    void setUp() {
        registry = new AuthzRegistry(null);
        port = new RecordingPort();
        deployer = new AuthzEntityDeployer(
            port,
            registry,
            new io.gravitee.gateway.services.sync.process.distributed.service.NoopDistributedSyncService()
        );
    }

    @Test
    void deploy_principal_is_unconditional_broadcast() {
        AuthzEntityReactorDeployable d = principal("idp.am.alice");

        deployer.deploy(d).blockingAwait();

        assertThat(port.entityOps).hasSize(1);
        assertThat(port.entityOps.peek().op()).isEqualTo("addOrUpdateEntity");
        assertThat(port.entityOps.peek().uid()).isEqualTo("Principal::\"idp.am.alice\"");
    }

    @Test
    void deploy_auto_derived_resource_skipped_when_API_not_in_registry() {
        AuthzEntityReactorDeployable d = resource("api.bookings");

        deployer.deploy(d).blockingAwait();

        assertThat(port.entityOps).isEmpty();
    }

    @Test
    void deploy_auto_derived_resource_proceeds_when_API_is_registered() {
        registry.register(List.of("api.bookings"));
        AuthzEntityReactorDeployable d = resource("api.bookings");

        deployer.deploy(d).blockingAwait();

        assertThat(port.entityOps).hasSize(1);
        assertThat(port.entityOps.peek().uid()).isEqualTo("Resource::\"api.bookings\"");
    }

    @Test
    void deploy_custom_resource_entity_broadcasts_unconditionally() {
        // C1: non-auto-derived RESOURCE entities aren't tied to any API and must land on
        // every gateway node regardless of which APIs the registry knows about.
        AuthzEntityReactorDeployable d = resource("team.acme.docs");

        deployer.deploy(d).blockingAwait();

        assertThat(port.entityOps).hasSize(1);
        assertThat(port.entityOps.peek().uid()).isEqualTo("Resource::\"team.acme.docs\"");
    }

    @Test
    void undeploy_does_not_filter_by_registry() {
        AuthzEntityReactorDeployable d = AuthzEntityReactorDeployable.builder()
            .entityId("api.bookings")
            .engineUid("Resource::\"api.bookings\"")
            .kind(AuthzEntityReactorDeployable.Kind.RESOURCE)
            .syncAction(SyncAction.UNDEPLOY)
            .build();

        deployer.undeploy(d).blockingAwait();

        assertThat(port.entityOps).hasSize(1);
        assertThat(port.entityOps.peek().op()).isEqualTo("removeEntity");
    }

    @Test
    void deploy_forwards_attributes_and_parents_to_the_engine_port() {
        registry.register(List.of("api.bookings"));
        AuthzEntityReactorDeployable d = AuthzEntityReactorDeployable.builder()
            .entityId("api.bookings")
            .engineUid("Resource::\"api.bookings\"")
            .kind(AuthzEntityReactorDeployable.Kind.RESOURCE)
            .attributes(Map.of("region", "eu"))
            .parents(List.of("Resource::\"api.parent\""))
            .syncAction(SyncAction.DEPLOY)
            .build();

        deployer.deploy(d).blockingAwait();

        RecordingPort.EntityOp op = port.entityOps.peek();
        assertThat(op.attributes()).containsEntry("region", "eu");
        assertThat(op.parents()).containsExactly("Resource::\"api.parent\"");
    }

    private static AuthzEntityReactorDeployable principal(String entityId) {
        return AuthzEntityReactorDeployable.builder()
            .entityId(entityId)
            .engineUid("Principal::\"" + entityId + "\"")
            .kind(AuthzEntityReactorDeployable.Kind.PRINCIPAL)
            .syncAction(SyncAction.DEPLOY)
            .build();
    }

    private static AuthzEntityReactorDeployable resource(String entityId) {
        return AuthzEntityReactorDeployable.builder()
            .entityId(entityId)
            .engineUid("Resource::\"" + entityId + "\"")
            .kind(AuthzEntityReactorDeployable.Kind.RESOURCE)
            .syncAction(SyncAction.DEPLOY)
            .build();
    }

    private static class RecordingPort implements AuthzEnginePort {

        record EntityOp(String op, String uid, Map<String, Object> attributes, List<String> parents) {}

        final ConcurrentLinkedQueue<EntityOp> entityOps = new ConcurrentLinkedQueue<>();

        @Override
        public Completable addOrUpdateEntity(String uid, Map<String, Object> attributes, List<String> parents) {
            entityOps.add(new EntityOp("addOrUpdateEntity", uid, attributes, parents));
            return Completable.complete();
        }

        @Override
        public Completable removeEntity(String uid) {
            entityOps.add(new EntityOp("removeEntity", uid, null, null));
            return Completable.complete();
        }

        @Override
        public Completable addOrUpdatePolicy(String docId, String name, String policyText) {
            return Completable.complete();
        }

        @Override
        public Completable removePolicy(String docId) {
            return Completable.complete();
        }

        @Override
        public Completable commit() {
            return Completable.complete();
        }
    }
}
