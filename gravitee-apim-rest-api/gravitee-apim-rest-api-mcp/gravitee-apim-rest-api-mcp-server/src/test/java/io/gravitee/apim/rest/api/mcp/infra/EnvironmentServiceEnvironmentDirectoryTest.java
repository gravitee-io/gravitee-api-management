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
package io.gravitee.apim.rest.api.mcp.infra;

import static io.gravitee.apim.rest.api.mcp.fakes.Fakes.MCP_CALLER;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import io.gravitee.apim.core.audit.model.AuditInfo;
import io.gravitee.apim.rest.api.mcp.core.model.EnvRef;
import io.gravitee.apim.rest.api.mcp.core.port.TriageAccess;
import io.gravitee.rest.api.model.EnvironmentEntity;
import io.gravitee.rest.api.service.EnvironmentService;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

class EnvironmentServiceEnvironmentDirectoryTest {

    private final EnvironmentService environmentService = mock(EnvironmentService.class);
    private final TriageAccess access = mock(TriageAccess.class);
    private final EnvironmentServiceEnvironmentDirectory directory = new EnvironmentServiceEnvironmentDirectory(environmentService, access);

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void should_split_the_environments_of_the_user_by_what_he_may_read() {
        when(environmentService.findByUser("DEFAULT", "user-id")).thenReturn(
            List.of(environment("e-prod", "prod", "Production"), environment("e-dev", null, "Dev"))
        );
        when(access.canReadTraffic(MCP_CALLER.on("e-prod"))).thenReturn(true);

        var environments = directory.environments(MCP_CALLER);

        assertThat(environments.readable()).containsExactly(new EnvRef("e-prod", "prod", "Production"));
        assertThat(environments.unreadable()).containsExactly(new EnvRef("e-dev", null, "Dev"));
    }

    @Test
    void should_list_every_environment_of_the_organization_for_an_organization_admin() {
        SecurityContextHolder.getContext().setAuthentication(
            new UsernamePasswordAuthenticationToken("user-id", null, List.of(new SimpleGrantedAuthority("ORGANIZATION:ADMIN")))
        );
        when(environmentService.findByUser("DEFAULT", null)).thenReturn(List.of(environment("e-prod", "prod", "Production")));
        when(access.canReadTraffic(MCP_CALLER.on("e-prod"))).thenReturn(true);

        assertThat(directory.environments(MCP_CALLER).readable()).extracting(EnvRef::id).containsExactly("e-prod");
    }

    private static EnvironmentEntity environment(String id, String hrid, String name) {
        var entity = new EnvironmentEntity();
        entity.setId(id);
        entity.setHrids(hrid == null ? List.of() : List.of(hrid));
        entity.setName(name);
        entity.setOrganizationId("DEFAULT");
        return entity;
    }
}
