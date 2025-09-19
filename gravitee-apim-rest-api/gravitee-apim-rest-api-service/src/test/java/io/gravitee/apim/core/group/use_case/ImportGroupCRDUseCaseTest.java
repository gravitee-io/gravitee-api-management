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
package io.gravitee.apim.core.group.use_case;

import static org.assertj.core.api.SoftAssertions.*;

import fixtures.core.model.AuditInfoFixtures;
import inmemory.CRDMembersDomainServiceInMemory;
import inmemory.GroupCrudServiceInMemory;
import inmemory.GroupQueryServiceInMemory;
import inmemory.RoleQueryServiceInMemory;
import inmemory.UserDomainServiceInMemory;
import io.gravitee.apim.core.audit.model.AuditInfo;
import io.gravitee.apim.core.group.model.crd.GroupCRDSpec;
import io.gravitee.apim.core.member.domain_service.ValidateCRDMembersDomainService;
import io.gravitee.apim.core.member.model.RoleScope;
import io.gravitee.apim.infra.domain_service.group.ValidateGroupCRDDomainServiceImpl;
import io.gravitee.rest.api.model.context.OriginContext;
import io.gravitee.rest.api.service.MembershipService;
import io.gravitee.rest.api.service.RoleService;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

/**
 * @author Antoine CORDIER (antoine.cordier at graviteesource.com)
 * @author GraviteeSource Team
 */
@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
public class ImportGroupCRDUseCaseTest {

    private static final String ORGANIZATION_ID = "organization-id";
    private static final String ENVIRONMENT_ID = "environment-id";
    private static final String ACTOR_USER_ID = "actor-user-id";
    private static final String GROUP_ID = "abc0a85b-9924-4981-bd71-69295353f5ff";

    private static final AuditInfo AUDIT_INFO = AuditInfoFixtures.anAuditInfo(ORGANIZATION_ID, ENVIRONMENT_ID, ACTOR_USER_ID);

    private final GroupQueryServiceInMemory groupQueryService = new GroupQueryServiceInMemory();

    private final GroupCrudServiceInMemory groupCrudService = new GroupCrudServiceInMemory();

    private final UserDomainServiceInMemory userDomainService = new UserDomainServiceInMemory();

    private final RoleQueryServiceInMemory roleQueryService = new RoleQueryServiceInMemory();

    private final CRDMembersDomainServiceInMemory membersService = new CRDMembersDomainServiceInMemory();

    private final MembershipService membershipService = Mockito.mock(MembershipService.class);

    private final RoleService roleService = Mockito.mock(RoleService.class);

    private ImportGroupCRDUseCase cut;

    @BeforeEach
    void setUp() {
        cut = new ImportGroupCRDUseCase(
            new ValidateGroupCRDDomainServiceImpl(new ValidateCRDMembersDomainService(userDomainService, roleQueryService), roleService),
            groupQueryService,
            groupCrudService,
            membersService
        );
    }

    @Test
    void should_create_group_setting_origin_to_kubernetes() {
        var spec = GroupCRDSpec.builder()
            .id("abc0a85b-9924-4981-bd71-69295353f5ff")
            .name("kubernetes-spec")
            .members(
                Set.of(
                    GroupCRDSpec.Member.builder()
                        .source("memory")
                        .sourceId("api1")
                        .roles(Map.of(RoleScope.API, "OWNER", RoleScope.APPLICATION, "OWNER", RoleScope.INTEGRATION, "OWNER"))
                        .build()
                )
            );

        cut.execute(new ImportGroupCRDUseCase.Input(AUDIT_INFO, spec.build()));

        var storage = groupCrudService.storage();

        assertSoftly(soft -> {
            soft.assertThat(storage).hasSize(1);
            soft.assertThat(storage.get(GROUP_ID)).isNotNull();
            soft.assertThat(storage.get(GROUP_ID).getOrigin()).isEqualTo(OriginContext.Origin.KUBERNETES.name());
        });
    }
}
