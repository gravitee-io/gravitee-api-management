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

import static fixtures.core.model.ApiFixtures.aMessageApiV4;
import static fixtures.core.model.MetadataFixtures.anApiMetadata;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

import inmemory.*;
import io.gravitee.apim.core.api.domain_service.ApiMetadataDecoderDomainService;
import io.gravitee.apim.core.api.domain_service.ApiMetadataDomainService;
import io.gravitee.apim.core.api.domain_service.ValidateApiMetadataDomainService;
import io.gravitee.apim.core.api.exception.ApiNotFoundException;
import io.gravitee.apim.core.api.exception.DuplicateApiMetadataKeyException;
import io.gravitee.apim.core.api.exception.DuplicateApiMetadataNameException;
import io.gravitee.apim.core.api.exception.InvalidApiMetadataValueException;
import io.gravitee.apim.core.api.model.Api;
import io.gravitee.apim.core.api.model.ApiMetadata;
import io.gravitee.apim.core.api.model.NewApiMetadata;
import io.gravitee.apim.core.audit.domain_service.AuditDomainService;
import io.gravitee.apim.core.audit.model.AuditActor;
import io.gravitee.apim.core.audit.model.AuditInfo;
import io.gravitee.apim.core.membership.domain_service.ApiPrimaryOwnerDomainService;
import io.gravitee.apim.core.membership.model.Membership;
import io.gravitee.apim.core.membership.model.Role;
import io.gravitee.apim.core.metadata.model.Metadata;
import io.gravitee.apim.core.user.model.BaseUserEntity;
import io.gravitee.apim.infra.json.jackson.JacksonJsonDiffProcessor;
import io.gravitee.apim.infra.template.FreemarkerTemplateProcessor;
import io.gravitee.rest.api.service.common.GraviteeContext;
import java.util.List;
import java.util.stream.Stream;
import org.junit.jupiter.api.*;

@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
public class CreateApiMetadataUseCaseTest {

    private final String API_ID = "api-id";
    private final String ORG_ID = "org-id";
    private final String ENV_ID = "env-id";
    private final AuditInfo AUDIT_INFO = AuditInfo
        .builder()
        .organizationId(ORG_ID)
        .environmentId(ENV_ID)
        .actor(AuditActor.builder().userId("user").build())
        .build();
    private final Api API = aMessageApiV4().toBuilder().id(API_ID).environmentId(ENV_ID).build();
    private final ApiCrudServiceInMemory apiCrudService = new ApiCrudServiceInMemory();
    AuditCrudServiceInMemory auditCrudService = new AuditCrudServiceInMemory();
    GroupQueryServiceInMemory groupQueryService = new GroupQueryServiceInMemory();
    MembershipCrudServiceInMemory membershipCrudService = new MembershipCrudServiceInMemory();
    MembershipQueryServiceInMemory membershipQueryService = new MembershipQueryServiceInMemory(membershipCrudService);
    RoleQueryServiceInMemory roleQueryService = new RoleQueryServiceInMemory();
    UserCrudServiceInMemory userCrudService = new UserCrudServiceInMemory();
    private final MetadataCrudServiceInMemory metadataCrudServiceInMemory = new MetadataCrudServiceInMemory();
    private ApiMetadataQueryServiceInMemory apiMetadataQueryService;
    private CreateApiMetadataUseCase createApiMetadataUseCase;

    @BeforeEach
    void setUp() {
        apiMetadataQueryService = new ApiMetadataQueryServiceInMemory(metadataCrudServiceInMemory);

        var auditDomainService = new AuditDomainService(auditCrudService, userCrudService, new JacksonJsonDiffProcessor());
        var apiMetadataDecoderDomainService = new ApiMetadataDecoderDomainService(
            apiMetadataQueryService,
            new FreemarkerTemplateProcessor()
        );
        var validateApiMetadataDomainService = new ValidateApiMetadataDomainService(
            apiMetadataQueryService,
            metadataCrudServiceInMemory,
            new ApiPrimaryOwnerDomainService(
                auditDomainService,
                groupQueryService,
                membershipCrudService,
                membershipQueryService,
                roleQueryService,
                userCrudService
            ),
            apiMetadataDecoderDomainService
        );
        var apiMetadataDomainService = new ApiMetadataDomainService(
            metadataCrudServiceInMemory,
            apiMetadataQueryService,
            auditDomainService
        );
        createApiMetadataUseCase = new CreateApiMetadataUseCase(validateApiMetadataDomainService, apiMetadataDomainService, apiCrudService);

        apiCrudService.initWith(List.of(API));
        this.initializePrimaryOwnerData();
    }

    @AfterEach
    void tearDown() {
        Stream
            .of(
                auditCrudService,
                membershipCrudService,
                roleQueryService,
                userCrudService,
                metadataCrudServiceInMemory,
                apiMetadataQueryService,
                apiCrudService,
                groupQueryService,
                membershipQueryService
            )
            .forEach(InMemoryAlternative::reset);

        GraviteeContext.cleanContext();
    }

    @Test
    void creates_api_metadata() {
        var expected = ApiMetadata
            .builder()
            .apiId(API_ID)
            .name("name")
            .value("value")
            .key("key")
            .format(Metadata.MetadataFormat.STRING)
            .build();
        var createdMetadata = createApiMetadataUseCase.execute(
            new CreateApiMetadataUseCase.Input(
                NewApiMetadata
                    .builder()
                    .apiId(API_ID)
                    .key("key")
                    .name("name")
                    .value("value")
                    .format(Metadata.MetadataFormat.STRING)
                    .build(),
                AUDIT_INFO
            )
        );
        assertThat(createdMetadata.created()).isNotNull();
        assertThat(createdMetadata.created()).usingRecursiveComparison().isEqualTo(expected);
    }

    @Test
    void creates_api_metadata_overriding_global_metadata() {
        apiMetadataQueryService.initWith(
            List.of(
                Metadata
                    .builder()
                    .referenceId("env-id")
                    .referenceType(Metadata.ReferenceType.ENVIRONMENT)
                    .name("global name")
                    .key("key")
                    .value("old-value")
                    .format(Metadata.MetadataFormat.STRING)
                    .build()
            )
        );
        var expected = ApiMetadata
            .builder()
            .apiId(API_ID)
            .name("name")
            .value("value")
            .defaultValue("old-value")
            .key("key")
            .format(Metadata.MetadataFormat.STRING)
            .build();
        var createdMetadata = createApiMetadataUseCase.execute(
            new CreateApiMetadataUseCase.Input(
                NewApiMetadata
                    .builder()
                    .apiId(API_ID)
                    .key("key")
                    .name("name")
                    .value("value")
                    .format(Metadata.MetadataFormat.STRING)
                    .build(),
                AUDIT_INFO
            )
        );
        assertThat(createdMetadata.created()).isNotNull();
        assertThat(createdMetadata.created()).usingRecursiveComparison().isEqualTo(expected);
    }

    @Test
    void cannot_create_if_api_not_in_env() {
        apiCrudService.reset();
        assertThrows(
            ApiNotFoundException.class,
            () ->
                createApiMetadataUseCase.execute(
                    new CreateApiMetadataUseCase.Input(
                        NewApiMetadata
                            .builder()
                            .apiId(API_ID)
                            .key("key")
                            .name("name")
                            .value("value")
                            .format(Metadata.MetadataFormat.STRING)
                            .build(),
                        AUDIT_INFO
                    )
                )
        );
    }

    @Test
    void cannot_create_with_duplicate_key() {
        apiMetadataQueryService.initWithApiMetadata(
            List.of(ApiMetadata.builder().apiId(API_ID).key("key").format(Metadata.MetadataFormat.STRING).build())
        );
        assertThrows(
            DuplicateApiMetadataKeyException.class,
            () ->
                createApiMetadataUseCase.execute(
                    new CreateApiMetadataUseCase.Input(
                        NewApiMetadata
                            .builder()
                            .apiId(API_ID)
                            .key("key")
                            .name("name")
                            .value("value")
                            .format(Metadata.MetadataFormat.STRING)
                            .build(),
                        AUDIT_INFO
                    )
                )
        );
    }

    @Test
    void cannot_create_with_duplicate_api_metadata_name() {
        apiMetadataQueryService.initWith(
            List.of(
                Metadata
                    .builder()
                    .name("name")
                    .key("key")
                    .referenceId(API_ID)
                    .referenceType(Metadata.ReferenceType.API)
                    .format(Metadata.MetadataFormat.STRING)
                    .build()
            )
        );
        assertThrows(
            DuplicateApiMetadataNameException.class,
            () ->
                createApiMetadataUseCase.execute(
                    new CreateApiMetadataUseCase.Input(
                        NewApiMetadata
                            .builder()
                            .apiId(API_ID)
                            .key("new key")
                            .name("name")
                            .value("value")
                            .format(Metadata.MetadataFormat.STRING)
                            .build(),
                        AUDIT_INFO
                    )
                )
        );
    }

    @Test
    void cannot_create_with_duplicate_environment_metadata_name() {
        apiMetadataQueryService.initWith(
            List.of(
                Metadata
                    .builder()
                    .name("nAmE")
                    .key("key")
                    .referenceId("env-id")
                    .referenceType(Metadata.ReferenceType.ENVIRONMENT)
                    .format(Metadata.MetadataFormat.STRING)
                    .build()
            )
        );
        assertThrows(
            DuplicateApiMetadataNameException.class,
            () ->
                createApiMetadataUseCase.execute(
                    new CreateApiMetadataUseCase.Input(
                        NewApiMetadata
                            .builder()
                            .apiId(API_ID)
                            .key("new key")
                            .name("name")
                            .value("value")
                            .format(Metadata.MetadataFormat.STRING)
                            .build(),
                        AUDIT_INFO
                    )
                )
        );
    }

    @Test
    void error_if_value_does_not_match_format() {
        assertThrows(
            InvalidApiMetadataValueException.class,
            () ->
                createApiMetadataUseCase.execute(
                    new CreateApiMetadataUseCase.Input(
                        NewApiMetadata
                            .builder()
                            .apiId(API_ID)
                            .key("new key")
                            .name("name")
                            .value("not an email")
                            .format(Metadata.MetadataFormat.MAIL)
                            .build(),
                        AUDIT_INFO
                    )
                )
        );
    }

    @Test
    void error_if_value_with_template_does_not_match_format() {
        assertThrows(
            InvalidApiMetadataValueException.class,
            () ->
                createApiMetadataUseCase.execute(
                    new CreateApiMetadataUseCase.Input(
                        NewApiMetadata
                            .builder()
                            .apiId(API_ID)
                            .key("new key")
                            .name("name")
                            .value("${api.version}")
                            .format(Metadata.MetadataFormat.MAIL)
                            .build(),
                        AUDIT_INFO
                    )
                )
        );
    }

    @Test
    void can_create_with_templated_value() {
        var createdApiMetadata = createApiMetadataUseCase.execute(
            new CreateApiMetadataUseCase.Input(
                NewApiMetadata
                    .builder()
                    .apiId(API_ID)
                    .key("new key")
                    .name("name")
                    .value("${api.primaryOwner.email}")
                    .format(Metadata.MetadataFormat.MAIL)
                    .build(),
                AUDIT_INFO
            )
        );
        assertThat(createdApiMetadata).isNotNull();
    }

    private void initializePrimaryOwnerData() {
        roleQueryService.initWith(
            List.of(
                Role
                    .builder()
                    .id("role-id")
                    .scope(Role.Scope.API)
                    .referenceType(Role.ReferenceType.ORGANIZATION)
                    .referenceId(ORG_ID)
                    .name("PRIMARY_OWNER")
                    .build()
            )
        );
        membershipQueryService.initWith(
            List.of(
                Membership
                    .builder()
                    .id("member-id")
                    .memberId("my-member-id")
                    .memberType(Membership.Type.USER)
                    .referenceType(Membership.ReferenceType.API)
                    .referenceId(API_ID)
                    .roleId("role-id")
                    .build()
            )
        );
        userCrudService.initWith(List.of(BaseUserEntity.builder().id("my-member-id").email("one_valid@email.com").build()));
    }
}
