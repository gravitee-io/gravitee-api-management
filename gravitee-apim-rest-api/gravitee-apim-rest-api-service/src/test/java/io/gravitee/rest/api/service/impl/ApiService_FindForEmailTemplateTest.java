/**
 * Copyright (C) 2015 The Gravitee team (http://gravitee.io)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.gravitee.rest.api.service.impl;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;

import io.gravitee.definition.model.Proxy;
import io.gravitee.repository.exceptions.TechnicalException;
import io.gravitee.repository.management.api.ApiRepository;
import io.gravitee.repository.management.model.Api;
import io.gravitee.rest.api.model.*;
import io.gravitee.rest.api.model.api.ApiEntity;
import io.gravitee.rest.api.model.permissions.RoleScope;
import io.gravitee.rest.api.model.permissions.SystemRole;
import io.gravitee.rest.api.service.*;
import io.gravitee.rest.api.service.common.ExecutionContext;
import io.gravitee.rest.api.service.configuration.flow.FlowService;
import io.gravitee.rest.api.service.converter.ApiConverter;
import io.gravitee.rest.api.service.notification.NotificationTemplateService;
import java.io.StringReader;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.assertj.core.api.Assertions;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

/**
 * @author Antoine CORDIER (antoine.cordier at graviteesource.com)
 * @author GraviteeSource Team
 */
@RunWith(MockitoJUnitRunner.class)
public class ApiService_FindForEmailTemplateTest {

    private static final String API_ID = "some-api";
    private static final String PO_ID = "some-primary-owner";
    private static final String EXPECTED_EMAIL = "some-po@gravitee.test";
    private static final String SUPPORT_EMAIL_METADATA_KEY = "email-support";
    private static final ExecutionContext EXECUTION_CONTEXT = new ExecutionContext("TEST", "TEST");

    @Mock
    private ApiRepository apiRepository;

    @Mock
    private ApiConverter apiConverter;

    @Mock
    private PlanService planService;

    @Mock
    private FlowService flowService;

    @Mock
    private ParameterService parameterService;

    @Mock
    private ApiMetadataService apiMetadataService;

    @Mock
    private UserService userService;

    @Mock
    private GroupService groupService;

    @Mock
    private MembershipService membershipService;

    @Mock
    NotificationTemplateService notificationTemplateService;

    @InjectMocks
    private ApiServiceImpl apiService = new ApiServiceImpl();

    @Test
    public void shouldFallbackToPrimaryOwnerGroupEmail() throws TechnicalException {
        when(apiRepository.findById(API_ID)).thenReturn(Optional.of(api()));
        when(apiConverter.toApiEntity(any(Api.class), any())).thenReturn(apiEntity());
        when(apiMetadataService.findAllByApi(API_ID)).thenReturn(List.of());
        when(groupService.findById(EXECUTION_CONTEXT, PO_ID)).thenReturn(poGroup());
        when(userService.findById(EXECUTION_CONTEXT, PO_ID)).thenReturn(poUser());

        when(
            notificationTemplateService.resolveInlineTemplateWithParam(
                eq(EXECUTION_CONTEXT.getOrganizationId()),
                anyString(),
                any(StringReader.class),
                any()
            )
        )
            .thenReturn("{email-support=}");

        when(
            membershipService.getPrimaryOwner(
                EXECUTION_CONTEXT.getOrganizationId(),
                io.gravitee.rest.api.model.MembershipReferenceType.API,
                API_ID
            )
        )
            .thenReturn(poGroupMembership());

        when(membershipService.getMembersByReferencesAndRole(eq(EXECUTION_CONTEXT), eq(MembershipReferenceType.GROUP), any(), any()))
            .thenReturn(Set.of(poGroupMember()));

        ApiModelEntity model = apiService.findByIdForTemplates(EXECUTION_CONTEXT, API_ID, true);

        assertThat(model.getMetadata().get(SUPPORT_EMAIL_METADATA_KEY)).isEqualTo(EXPECTED_EMAIL);
    }

    private static Api api() {
        final Api api = new Api();
        api.setId(API_ID);
        api.setEnvironmentId(EXECUTION_CONTEXT.getEnvironmentId());
        return api;
    }

    private static ApiEntity apiEntity() {
        final ApiEntity api = new ApiEntity();
        api.setId(API_ID);
        api.setEnvironmentId(EXECUTION_CONTEXT.getEnvironmentId());
        Proxy proxy = new Proxy();
        proxy.setVirtualHosts(List.of());
        api.setProxy(proxy);
        return api;
    }

    private static MembershipEntity poGroupMembership() {
        final MembershipEntity membership = new MembershipEntity();
        membership.setMemberId(PO_ID);
        membership.setMemberType(MembershipMemberType.GROUP);
        return membership;
    }

    private static GroupEntity poGroup() {
        final GroupEntity group = new GroupEntity();
        group.setId(PO_ID);
        return group;
    }

    private static UserEntity poUser() {
        final UserEntity user = new UserEntity();
        user.setId(PO_ID);
        user.setEmail(EXPECTED_EMAIL);
        return user;
    }

    private static MemberEntity poGroupMember() {
        final MemberEntity member = new MemberEntity();
        member.setId(PO_ID);
        member.setReferenceId(PO_ID);
        member.setRoles(List.of(poRole()));
        return member;
    }

    private static RoleEntity poRole() {
        final RoleEntity role = new RoleEntity();
        role.setId(PO_ID);
        role.setScope(RoleScope.API);
        role.setName(SystemRole.PRIMARY_OWNER.name());
        return role;
    }
}
