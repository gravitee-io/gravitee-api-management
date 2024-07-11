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

import static assertions.CoreAssertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import fixtures.core.model.ApiFixtures;
import fixtures.core.model.AuditInfoFixtures;
import inmemory.ApiCategoryQueryServiceInMemory;
import inmemory.ApiCrudServiceInMemory;
import inmemory.ApiMetadataQueryServiceInMemory;
import inmemory.AuditCrudServiceInMemory;
import inmemory.GroupQueryServiceInMemory;
import inmemory.InMemoryAlternative;
import inmemory.IndexerInMemory;
import inmemory.MembershipCrudServiceInMemory;
import inmemory.MembershipQueryServiceInMemory;
import inmemory.MetadataCrudServiceInMemory;
import inmemory.RoleQueryServiceInMemory;
import inmemory.UserCrudServiceInMemory;
import io.gravitee.apim.core.api.domain_service.ApiIndexerDomainService;
import io.gravitee.apim.core.api.domain_service.ApiMetadataDecoderDomainService;
import io.gravitee.apim.core.api.domain_service.CategoryDomainService;
import io.gravitee.apim.core.api.domain_service.GroupValidationService;
import io.gravitee.apim.core.api.domain_service.UpdateFederatedApiDomainService;
import io.gravitee.apim.core.api.domain_service.ValidateFederatedApiDomainService;
import io.gravitee.apim.core.api.model.Api;
import io.gravitee.apim.core.audit.domain_service.AuditDomainService;
import io.gravitee.apim.core.group.model.Group;
import io.gravitee.apim.core.membership.domain_service.ApiPrimaryOwnerDomainService;
import io.gravitee.apim.core.membership.model.Membership;
import io.gravitee.apim.core.user.model.BaseUserEntity;
import io.gravitee.apim.infra.json.jackson.JacksonJsonDiffProcessor;
import io.gravitee.apim.infra.template.FreemarkerTemplateProcessor;
import io.gravitee.rest.api.model.CategoryEntity;
import java.util.List;
import java.util.Set;
import java.util.stream.Stream;
import org.assertj.core.api.SoftAssertions;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class UpdateFederatedApiUseCaseTest {

    private static final String API_ID = "my-api";
    private static final String ORGANIZATION_ID = "organization-id";
    private static final String USER_ID = "user-id";
    private static final String ENVIRONMENT_ID = "environment-id";

    AuditCrudServiceInMemory auditCrudService = new AuditCrudServiceInMemory();
    ApiCrudServiceInMemory apiCrudService = new ApiCrudServiceInMemory();
    UserCrudServiceInMemory userCrudService = new UserCrudServiceInMemory();
    RoleQueryServiceInMemory roleQueryService = new RoleQueryServiceInMemory();
    GroupQueryServiceInMemory groupQueryService = new GroupQueryServiceInMemory();
    MembershipCrudServiceInMemory membershipCrudService = new MembershipCrudServiceInMemory();
    MetadataCrudServiceInMemory metadataCrudService = new MetadataCrudServiceInMemory();
    MembershipQueryServiceInMemory membershipQueryService = new MembershipQueryServiceInMemory(membershipCrudService);
    IndexerInMemory indexer = new IndexerInMemory();
    UpdateFederatedApiUseCase usecase;
    CategoryDomainService categoryDomainService = mock(CategoryDomainService.class);

    @BeforeEach
    void setUp() {
        roleQueryService.resetSystemRoles(ORGANIZATION_ID);
        membershipQueryService.initWith(
            List.of(
                Membership
                    .builder()
                    .id("member-id")
                    .memberId("my-member-id")
                    .memberType(Membership.Type.USER)
                    .referenceType(Membership.ReferenceType.API)
                    .referenceId(API_ID)
                    .roleId("api-po-id-organization-id")
                    .build()
            )
        );
        groupQueryService.initWith(
            List.of(
                Group
                    .builder()
                    .id("group-1")
                    .environmentId("environment-id")
                    .eventRules(List.of(new Group.GroupEventRule(Group.GroupEvent.API_CREATE)))
                    .build()
            )
        );
        userCrudService.initWith(List.of(BaseUserEntity.builder().id("my-member-id").email("one_valid@email.com").build()));

        var auditDomainService = new AuditDomainService(auditCrudService, userCrudService, new JacksonJsonDiffProcessor());
        var apiPrimaryOwnerService = new ApiPrimaryOwnerDomainService(
            auditDomainService,
            groupQueryService,
            membershipCrudService,
            membershipQueryService,
            roleQueryService,
            userCrudService
        );
        var updateFederatedApiDomainService = new UpdateFederatedApiDomainService(
            apiCrudService,
            auditDomainService,
            new ValidateFederatedApiDomainService(new GroupValidationService(groupQueryService), categoryDomainService),
            categoryDomainService,
            new ApiIndexerDomainService(
                new ApiMetadataDecoderDomainService(
                    new ApiMetadataQueryServiceInMemory(metadataCrudService),
                    new FreemarkerTemplateProcessor()
                ),
                apiPrimaryOwnerService,
                new ApiCategoryQueryServiceInMemory(),
                indexer
            )
        );
        usecase = new UpdateFederatedApiUseCase(apiPrimaryOwnerService, updateFederatedApiDomainService);
    }

    @AfterEach
    void tearDown() {
        Stream
            .of(
                auditCrudService,
                apiCrudService,
                userCrudService,
                roleQueryService,
                groupQueryService,
                membershipCrudService,
                metadataCrudService,
                membershipQueryService,
                indexer
            )
            .forEach(InMemoryAlternative::reset);
    }

    @Test
    public void update_federation_api_with_basic_configuration_info() {
        //given
        apiCrudService.initWith(List.of(ApiFixtures.aFederatedApi()));
        var auditInfo = AuditInfoFixtures.anAuditInfo(ORGANIZATION_ID, ENVIRONMENT_ID, USER_ID);
        String categoryKey = "categoryKey-1";
        var apiToUpdate = ApiFixtures
            .aFederatedApi()
            .toBuilder()
            .name("updated-name")
            .description("updated-description")
            .version("2.0.0")
            .labels(List.of("label-1"))
            .categories(Set.of(categoryKey))
            .apiLifecycleState(Api.ApiLifecycleState.PUBLISHED)
            .build();

        CategoryEntity categoryEntity = new CategoryEntity();
        String categoryId = "categoryId-1";
        categoryEntity.setId(categoryId);
        when(categoryDomainService.toCategoryId(any(), any())).thenReturn(Set.of(categoryId));
        when(categoryDomainService.toCategoryKey(any(), any())).thenReturn(Set.of("key-1"));

        //when
        var output = usecase.execute(UpdateFederatedApiUseCase.Input.builder().apiToUpdate(apiToUpdate).auditInfo(auditInfo).build());
        //then
        SoftAssertions.assertSoftly(soft -> {
            var updatedApi = output.updatedApi();
            assertThat(updatedApi.getName()).isEqualTo("updated-name");
            assertThat(updatedApi.getDescription()).isEqualTo("updated-description");
            assertThat(updatedApi.getVersion()).isEqualTo("2.0.0");
            assertThat(updatedApi.getLabels()).containsExactly("label-1");
            assertThat(updatedApi.getCategories()).containsExactly("key-1");
            assertThat(updatedApi.getApiLifecycleState()).isEqualTo(Api.ApiLifecycleState.PUBLISHED);
        });
    }

    @Test
    public void update_creates_audit_log() {
        apiCrudService.initWith(List.of(ApiFixtures.aFederatedApi()));
        var auditInfo = AuditInfoFixtures.anAuditInfo(ORGANIZATION_ID, ENVIRONMENT_ID, USER_ID);
        var apiToUpdate = ApiFixtures.aFederatedApi();

        usecase.execute(UpdateFederatedApiUseCase.Input.builder().apiToUpdate(apiToUpdate).auditInfo(auditInfo).build());

        assertThat(auditCrudService.storage()).isNotEmpty().hasSize(1);
    }

    @Test
    public void update_creates_index() {
        apiCrudService.initWith(List.of(ApiFixtures.aFederatedApi()));
        var auditInfo = AuditInfoFixtures.anAuditInfo(ORGANIZATION_ID, ENVIRONMENT_ID, USER_ID);
        var apiToUpdate = ApiFixtures.aFederatedApi();

        usecase.execute(UpdateFederatedApiUseCase.Input.builder().apiToUpdate(apiToUpdate).auditInfo(auditInfo).build());

        assertThat(indexer.storage()).isNotEmpty().hasSize(1);
    }
}
