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
package io.gravitee.apim.core.api.domain_service;

import static org.assertj.core.api.Assertions.*;

import inmemory.*;
import io.gravitee.apim.core.api.exception.InvalidApiMetadataValueException;
import io.gravitee.apim.core.api.model.Api;
import io.gravitee.apim.core.audit.domain_service.AuditDomainService;
import io.gravitee.apim.core.membership.domain_service.ApiPrimaryOwnerDomainService;
import io.gravitee.apim.core.membership.model.Membership;
import io.gravitee.apim.core.membership.model.Role;
import io.gravitee.apim.core.metadata.model.Metadata;
import io.gravitee.apim.core.user.model.BaseUserEntity;
import io.gravitee.apim.infra.json.jackson.JacksonJsonDiffProcessor;
import io.gravitee.apim.infra.template.FreemarkerTemplateProcessor;
import java.util.List;
import java.util.stream.Stream;
import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

class ValidateApiMetadataDomainServiceTest {

    private static final String API_ID = "my-api";
    private static final String ORGANIZATION_ID = "organization-id";
    AuditCrudServiceInMemory auditCrudService = new AuditCrudServiceInMemory();
    UserCrudServiceInMemory userCrudService = new UserCrudServiceInMemory();
    GroupQueryServiceInMemory groupQueryService = new GroupQueryServiceInMemory();
    MembershipCrudServiceInMemory membershipCrudService = new MembershipCrudServiceInMemory();
    MembershipQueryServiceInMemory membershipQueryService = new MembershipQueryServiceInMemory(membershipCrudService);
    RoleQueryServiceInMemory roleQueryService = new RoleQueryServiceInMemory();
    MetadataCrudServiceInMemory metadataCrudService = new MetadataCrudServiceInMemory();
    ApiMetadataQueryServiceInMemory apiMetadataQueryService;
    ValidateApiMetadataDomainService service;

    @BeforeEach
    void setUp() {
        var auditDomainService = new AuditDomainService(auditCrudService, userCrudService, new JacksonJsonDiffProcessor());
        var apiMetadataDecoderDomainService = new ApiMetadataDecoderDomainService(
            apiMetadataQueryService,
            new FreemarkerTemplateProcessor()
        );
        service = new ValidateApiMetadataDomainService(
            apiMetadataQueryService,
            metadataCrudService,
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

        roleQueryService.initWith(
            List.of(
                Role.builder()
                    .id("role-id")
                    .scope(Role.Scope.API)
                    .referenceType(Role.ReferenceType.ORGANIZATION)
                    .referenceId(ORGANIZATION_ID)
                    .name("PRIMARY_OWNER")
                    .build()
            )
        );
        membershipQueryService.initWith(
            List.of(
                Membership.builder()
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

        metadataCrudService.initWith(
            List.of(
                Metadata.builder().referenceId(API_ID).referenceType(Metadata.ReferenceType.API).key("exists").value("john@doe.com").build()
            )
        );
    }

    @AfterEach
    void tearDown() {
        Stream.of(
            auditCrudService,
            membershipCrudService,
            roleQueryService,
            userCrudService,
            metadataCrudService,
            groupQueryService,
            membershipQueryService
        ).forEach(InMemoryAlternative::reset);
    }

    @Nested
    class ValidateValueByFormat {

        @ParameterizedTest
        @MethodSource("provideValidParameters")
        void should_validate_value(final String value, final Metadata.MetadataFormat format) {
            assertThatCode(() ->
                service.validateValueByFormat(Api.builder().build(), ORGANIZATION_ID, value, format)
            ).doesNotThrowAnyException();
        }

        public static Stream<Arguments> provideValidParameters() {
            return Stream.of(
                Arguments.of("john@doe.com", Metadata.MetadataFormat.MAIL),
                Arguments.of("https://my-url.com", Metadata.MetadataFormat.URL),
                Arguments.of("https://my-url", Metadata.MetadataFormat.URL),
                Arguments.of("2005-01-01", Metadata.MetadataFormat.DATE),
                Arguments.of("2021-10-06T15:17:15.282+00:00", Metadata.MetadataFormat.DATE),
                Arguments.of("42", Metadata.MetadataFormat.NUMERIC),
                Arguments.of("100.5", Metadata.MetadataFormat.NUMERIC),
                Arguments.of("true", Metadata.MetadataFormat.BOOLEAN),
                Arguments.of("false", Metadata.MetadataFormat.BOOLEAN),
                Arguments.of("any value for boolean works", Metadata.MetadataFormat.BOOLEAN),
                Arguments.of("100", Metadata.MetadataFormat.STRING)
            );
        }

        @ParameterizedTest
        @MethodSource("provideInvalidParameters")
        void should_throw_error_on_invalid_value(final String value, final Metadata.MetadataFormat format) {
            assertThatThrownBy(() ->
                service.validateValueByFormat(Api.builder().id(API_ID).build(), ORGANIZATION_ID, value, format)
            ).isInstanceOf(InvalidApiMetadataValueException.class);
        }

        public static Stream<Arguments> provideInvalidParameters() {
            return Stream.of(
                Arguments.of("johndoe.com", Metadata.MetadataFormat.MAIL),
                Arguments.of("@johndoe.com", Metadata.MetadataFormat.MAIL),
                Arguments.of("my-url", Metadata.MetadataFormat.URL),
                Arguments.of("my-url.com", Metadata.MetadataFormat.URL),
                Arguments.of("2005", Metadata.MetadataFormat.DATE),
                Arguments.of("1/2", Metadata.MetadataFormat.NUMERIC)
            );
        }
    }

    @Test
    void should_throw_error_when_decoding_mail_value_referencing_another_metadata_key() {
        assertThatThrownBy(() ->
            service.validateValueByFormat(
                Api.builder().id(API_ID).build(),
                ORGANIZATION_ID,
                "${api.metadata['exists']}",
                Metadata.MetadataFormat.MAIL
            )
        ).isInstanceOf(InvalidApiMetadataValueException.class);
    }

    @Test
    void should_not_throw_error_when_decoding_string_value_referencing_another_metadata_key() {
        assertThatCode(() ->
            service.validateValueByFormat(
                Api.builder().id(API_ID).build(),
                ORGANIZATION_ID,
                "${api.metadata['exists']}",
                Metadata.MetadataFormat.STRING
            )
        ).doesNotThrowAnyException();
    }
}
