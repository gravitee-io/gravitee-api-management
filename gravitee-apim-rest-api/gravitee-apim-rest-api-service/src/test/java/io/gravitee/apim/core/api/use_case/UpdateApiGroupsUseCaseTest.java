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
package io.gravitee.apim.core.api.use_case;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import fixtures.core.model.ApiFixtures;
import inmemory.ApiCrudServiceInMemory;
import inmemory.AuditCrudServiceInMemory;
import inmemory.GroupQueryServiceInMemory;
import inmemory.UserCrudServiceInMemory;
import io.gravitee.apim.core.api.exception.ApiNotFoundException;
import io.gravitee.apim.core.audit.domain_service.AuditDomainService;
import io.gravitee.apim.core.audit.model.AuditActor;
import io.gravitee.apim.core.audit.model.AuditEntity;
import io.gravitee.apim.core.audit.model.AuditInfo;
import io.gravitee.apim.core.audit.model.AuditProperties;
import io.gravitee.apim.core.audit.model.event.ApiAuditEvent;
import io.gravitee.apim.core.exception.ValidationDomainException;
import io.gravitee.apim.core.group.domain_service.ValidateGroupsDomainService;
import io.gravitee.apim.core.group.model.Group;
import io.gravitee.apim.infra.json.jackson.JacksonJsonDiffProcessor;
import io.gravitee.common.utils.TimeProvider;
import io.gravitee.rest.api.model.context.OriginContext;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Stream;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class UpdateApiGroupsUseCaseTest {

    private static final String API_ID = "api-1";
    private static final String ENV_ID = "env-1";
    private static final String ORG_ID = "org-1";
    private static final String USER_ID = "user-1";
    private static final Instant INSTANT_NOW = Instant.parse("2026-03-25T10:00:00Z");

    private final ApiCrudServiceInMemory apiCrudService = new ApiCrudServiceInMemory();
    private final AuditCrudServiceInMemory auditCrudService = new AuditCrudServiceInMemory();
    private final UserCrudServiceInMemory userCrudService = new UserCrudServiceInMemory();
    private final GroupQueryServiceInMemory groupQueryService = new GroupQueryServiceInMemory();

    private UpdateApiGroupsUseCase useCase;

    @BeforeAll
    static void beforeAll() {
        TimeProvider.overrideClock(Clock.fixed(INSTANT_NOW, ZoneId.of("UTC")));
    }

    @AfterAll
    static void afterAll() {
        TimeProvider.overrideClock(Clock.systemDefaultZone());
    }

    @BeforeEach
    void setUp() {
        var auditDomainService = new AuditDomainService(auditCrudService, userCrudService, new JacksonJsonDiffProcessor());
        var validateGroupsDomainService = new ValidateGroupsDomainService(groupQueryService);
        useCase = new UpdateApiGroupsUseCase(apiCrudService, auditDomainService, validateGroupsDomainService);
        apiCrudService.initWith(
            List.of(ApiFixtures.aProxyApiV4().toBuilder().id(API_ID).environmentId(ENV_ID).groups(Set.of("old-group")).build())
        );
        groupQueryService.initWith(
            List.of(
                Group.builder().id("group-1").name("Group 1").environmentId(ENV_ID).build(),
                Group.builder().id("group-2").name("Group 2").environmentId(ENV_ID).build()
            )
        );
    }

    @AfterEach
    void tearDown() {
        Stream.of(apiCrudService, auditCrudService, userCrudService, groupQueryService).forEach(s -> s.reset());
    }

    @Test
    void should_update_api_groups_and_create_audit_log() {
        var auditInfo = anAuditInfo(ENV_ID);
        var input = new UpdateApiGroupsUseCase.Input(API_ID, Set.of("group-1", "group-2"), auditInfo);

        var output = useCase.execute(input);

        // Groups updated in storage
        var updatedApi = apiCrudService.get(API_ID);
        assertThat(updatedApi.getGroups()).containsExactlyInAnyOrder("group-1", "group-2");
        assertThat(output.groups()).containsExactlyInAnyOrder("group-1", "group-2");

        // Audit log created
        assertThat(auditCrudService.storage())
            .usingRecursiveFieldByFieldElementComparatorIgnoringFields("id", "patch")
            .contains(
                new AuditEntity(
                    "generated-id",
                    ORG_ID,
                    ENV_ID,
                    AuditEntity.AuditReferenceType.API,
                    API_ID,
                    USER_ID,
                    Map.of(AuditProperties.API.name(), API_ID),
                    ApiAuditEvent.API_UPDATED.name(),
                    INSTANT_NOW.atZone(ZoneId.of("UTC")),
                    ""
                )
            );
    }

    @Test
    void should_throw_when_api_not_found() {
        var input = new UpdateApiGroupsUseCase.Input("unknown-api", Set.of("group-1"), anAuditInfo(ENV_ID));

        assertThatThrownBy(() -> useCase.execute(input)).isInstanceOf(ApiNotFoundException.class);
    }

    @Test
    void should_throw_when_api_belongs_to_different_environment() {
        var input = new UpdateApiGroupsUseCase.Input(API_ID, Set.of("group-1"), anAuditInfo("other-env"));

        assertThatThrownBy(() -> useCase.execute(input)).isInstanceOf(ApiNotFoundException.class);
    }

    @Test
    void should_throw_when_api_is_kubernetes_managed() {
        apiCrudService.reset();
        apiCrudService.initWith(
            List.of(
                ApiFixtures.aProxyApiV4()
                    .toBuilder()
                    .id(API_ID)
                    .environmentId(ENV_ID)
                    .groups(Set.of("old-group"))
                    .originContext(new OriginContext.Kubernetes(OriginContext.Kubernetes.Mode.FULLY_MANAGED))
                    .build()
            )
        );

        var input = new UpdateApiGroupsUseCase.Input(API_ID, Set.of("group-1"), anAuditInfo(ENV_ID));

        assertThatThrownBy(() -> useCase.execute(input))
            .isInstanceOf(ValidationDomainException.class)
            .hasMessageContaining("Kubernetes-managed API");
    }

    @Test
    void should_filter_out_nonexistent_groups() {
        var auditInfo = anAuditInfo(ENV_ID);
        var input = new UpdateApiGroupsUseCase.Input(API_ID, Set.of("group-1", "nonexistent-group"), auditInfo);

        var output = useCase.execute(input);

        // Only the valid group should be persisted
        var updatedApi = apiCrudService.get(API_ID);
        assertThat(updatedApi.getGroups()).containsExactly("group-1");
        assertThat(output.groups()).containsExactly("group-1");
    }

    private static AuditInfo anAuditInfo(String envId) {
        return AuditInfo.builder().organizationId(ORG_ID).environmentId(envId).actor(AuditActor.builder().userId(USER_ID).build()).build();
    }
}
