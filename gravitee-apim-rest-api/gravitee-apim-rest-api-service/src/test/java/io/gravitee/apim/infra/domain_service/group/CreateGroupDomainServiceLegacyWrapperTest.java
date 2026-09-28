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
package io.gravitee.apim.infra.domain_service.group;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import fixtures.core.model.AuditInfoFixtures;
import fixtures.core.model.GroupFixtures;
import inmemory.GroupQueryServiceInMemory;
import io.gravitee.apim.core.audit.model.AuditInfo;
import io.gravitee.apim.core.group.domain_service.CreateGroupDomainService.CreateResult;
import io.gravitee.rest.api.model.GroupEntity;
import io.gravitee.rest.api.model.NewGroupEntity;
import io.gravitee.rest.api.service.GroupService;
import io.gravitee.rest.api.service.common.ExecutionContext;
import io.gravitee.rest.api.service.exceptions.GroupNameAlreadyExistsException;
import java.util.Date;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
class CreateGroupDomainServiceLegacyWrapperTest {

    private static final String ORGANIZATION_ID = "organization-id";
    private static final String ENVIRONMENT_ID = "environment-id";
    private static final AuditInfo AUDIT_INFO = AuditInfoFixtures.anAuditInfo(ORGANIZATION_ID, ENVIRONMENT_ID, "user-id");

    @Mock
    private GroupService groupService;

    private final GroupQueryServiceInMemory groupQueryService = new GroupQueryServiceInMemory();
    private CreateGroupDomainServiceLegacyWrapper service;

    @BeforeEach
    void setUp() {
        service = new CreateGroupDomainServiceLegacyWrapper(groupService, groupQueryService);
    }

    @AfterEach
    void tearDown() {
        groupQueryService.reset();
    }

    @Test
    void should_delegate_empty_group_creation_to_legacy_group_service_and_return_was_created_true() {
        when(groupService.create(any(ExecutionContext.class), any(NewGroupEntity.class))).thenReturn(
            GroupEntity.builder()
                .id("created-group-id")
                .name("Helios")
                .environmentId(ENVIRONMENT_ID)
                .createdAt(new Date())
                .updatedAt(new Date())
                .build()
        );

        CreateResult result = service.createEmpty("Helios", AUDIT_INFO);

        assertThat(result.wasCreated()).isTrue();
        assertThat(result.group().getId()).isEqualTo("created-group-id");
        assertThat(result.group().getName()).isEqualTo("Helios");
        assertThat(result.group().getEnvironmentId()).isEqualTo(ENVIRONMENT_ID);

        var executionContextCaptor = ArgumentCaptor.forClass(ExecutionContext.class);
        var newGroupCaptor = ArgumentCaptor.forClass(NewGroupEntity.class);
        verify(groupService).create(executionContextCaptor.capture(), newGroupCaptor.capture());
        assertThat(executionContextCaptor.getValue().getOrganizationId()).isEqualTo(ORGANIZATION_ID);
        assertThat(executionContextCaptor.getValue().getEnvironmentId()).isEqualTo(ENVIRONMENT_ID);
        assertThat(newGroupCaptor.getValue().getName()).isEqualTo("Helios");
    }

    @Test
    void should_reuse_existing_group_when_name_already_exists_and_return_was_created_false() {
        when(groupService.create(eq(new ExecutionContext(ORGANIZATION_ID, ENVIRONMENT_ID)), any(NewGroupEntity.class))).thenThrow(
            new GroupNameAlreadyExistsException("Helios")
        );
        groupQueryService.initWith(
            List.of(GroupFixtures.aGroup("existing-group-id").toBuilder().name("Helios").environmentId(ENVIRONMENT_ID).build())
        );

        CreateResult result = service.createEmpty("Helios", AUDIT_INFO);

        assertThat(result.wasCreated()).isFalse();
        assertThat(result.group().getId()).isEqualTo("existing-group-id");
        assertThat(result.group().getName()).isEqualTo("Helios");
    }

    @Test
    void should_throw_when_name_is_null() {
        assertThatThrownBy(() -> service.createEmpty(null, AUDIT_INFO))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("Group name must not be null or blank");

        verify(groupService, never()).create(any(), any());
    }

    @Test
    void should_throw_when_name_is_blank() {
        assertThatThrownBy(() -> service.createEmpty("   ", AUDIT_INFO))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("Group name must not be null or blank");

        verify(groupService, never()).create(any(), any());
    }
}
