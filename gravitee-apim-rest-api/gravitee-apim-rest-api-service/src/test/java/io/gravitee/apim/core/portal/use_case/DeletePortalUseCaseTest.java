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
import static org.assertj.core.api.Assertions.catchThrowable;

import fixtures.core.model.PortalFixtures;
import inmemory.PortalCrudServiceInMemory;
import io.gravitee.apim.core.audit.model.AuditActor;
import io.gravitee.apim.core.audit.model.AuditInfo;
import io.gravitee.apim.core.portal.exception.PortalNotFoundException;
import io.gravitee.apim.core.portal.model.PortalId;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Test;

@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
class DeletePortalUseCaseTest {

    private static final AuditInfo AUDIT_INFO = AuditInfo.builder()
        .organizationId("organization-id")
        .environmentId("environment-id")
        .actor(AuditActor.builder().userId("user-id").build())
        .build();

    private final PortalCrudServiceInMemory portalCrudService = new PortalCrudServiceInMemory();
    private DeletePortalUseCase useCase;

    @BeforeEach
    void setUp() {
        useCase = new DeletePortalUseCase(portalCrudService);
    }

    @AfterEach
    void tearDown() {
        portalCrudService.reset();
    }

    @Test
    void should_delete() {
        var portal = PortalFixtures.aPortal();
        portalCrudService.initWith(List.of(portal));

        useCase.execute(new DeletePortalUseCase.Input(AUDIT_INFO, portal.getId()));

        assertThat(portalCrudService.storage()).isEmpty();
    }

    @Test
    void should_throw_when_missing() {
        var unknownId = PortalId.of("00000000-0000-0000-0000-0000000000ff");
        var throwable = catchThrowable(() -> useCase.execute(new DeletePortalUseCase.Input(AUDIT_INFO, unknownId)));

        assertThat(throwable).isInstanceOf(PortalNotFoundException.class).hasMessage("Portal [ " + unknownId + " ] not found");
    }

    @Test
    void should_throw_when_id_exists_in_different_environment() {
        var portal = PortalFixtures.aPortal();
        portalCrudService.initWith(List.of(portal));

        var otherEnvAudit = AuditInfo.builder()
            .organizationId("organization-id")
            .environmentId("other-env")
            .actor(AuditActor.builder().userId("user-id").build())
            .build();

        var throwable = catchThrowable(() -> useCase.execute(new DeletePortalUseCase.Input(otherEnvAudit, portal.getId())));

        assertThat(throwable).isInstanceOf(PortalNotFoundException.class);
        assertThat(portalCrudService.storage()).containsExactly(portal);
    }
}
