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
package io.gravitee.apim.core.portal.use_case;

import static org.assertj.core.api.Assertions.assertThat;

import fixtures.core.model.PortalFixtures;
import inmemory.PortalCrudServiceInMemory;
import io.gravitee.apim.core.audit.model.AuditActor;
import io.gravitee.apim.core.audit.model.AuditInfo;
import io.gravitee.apim.core.portal.model.Portal;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Test;

@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
class CreateOrUpdatePortalUseCaseTest {

    private static final AuditInfo AUDIT_INFO = AuditInfo.builder()
        .organizationId("organization-id")
        .environmentId("environment-id")
        .actor(AuditActor.builder().userId("user-id").build())
        .build();

    private final PortalCrudServiceInMemory portalCrudService = new PortalCrudServiceInMemory();
    private CreateOrUpdatePortalUseCase useCase;

    @BeforeEach
    void setUp() {
        useCase = new CreateOrUpdatePortalUseCase(portalCrudService);
    }

    @AfterEach
    void tearDown() {
        portalCrudService.reset();
    }

    @Test
    void should_create_when_not_existing() {
        var portal = PortalFixtures.aPortal();

        var output = useCase.execute(new CreateOrUpdatePortalUseCase.Input(AUDIT_INFO, portal));

        assertThat(output.portal()).isEqualTo(portal);
        assertThat(portalCrudService.storage()).containsExactly(portal);
    }

    @Test
    void should_update_when_existing() {
        var existing = PortalFixtures.aPortal();
        portalCrudService.initWith(java.util.List.of(existing));
        var renamed = existing.toBuilder().name("Renamed Portal").build();

        var output = useCase.execute(new CreateOrUpdatePortalUseCase.Input(AUDIT_INFO, renamed));

        assertThat(output.portal().getName()).isEqualTo("Renamed Portal");
        assertThat(portalCrudService.storage()).hasSize(1).containsExactly(renamed);
    }

    @Test
    void should_be_idempotent_when_put_twice() {
        var portal = PortalFixtures.aPortal();

        var first = useCase.execute(new CreateOrUpdatePortalUseCase.Input(AUDIT_INFO, portal));
        var second = useCase.execute(new CreateOrUpdatePortalUseCase.Input(AUDIT_INFO, portal));

        assertThat(first.portal()).isEqualTo(second.portal());
        assertThat(portalCrudService.storage()).hasSize(1).containsExactly(portal);
    }

    @Test
    void should_not_consider_an_id_match_in_a_different_environment_as_existing() {
        var existing = PortalFixtures.aPortal();
        portalCrudService.initWith(java.util.List.of(existing));

        var otherEnvAudit = AuditInfo.builder()
            .organizationId("organization-id")
            .environmentId("other-env")
            .actor(AuditActor.builder().userId("user-id").build())
            .build();
        var inOtherEnv = Portal.builder()
            .id(existing.getId())
            .environmentId("other-env")
            .organizationId("organization-id")
            .name("Other Env Portal")
            .build();

        var output = useCase.execute(new CreateOrUpdatePortalUseCase.Input(otherEnvAudit, inOtherEnv));

        assertThat(output.portal()).isEqualTo(inOtherEnv);
        assertThat(portalCrudService.storage()).hasSize(2);
        assertThat(portalCrudService.storage().stream().map(Portal::getEnvironmentId).toList()).containsExactlyInAnyOrder(
            "environment-id",
            "other-env"
        );
    }
}
