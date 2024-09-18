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
package io.gravitee.apim.core.application;

import static io.gravitee.apim.core.member.model.SystemRole.PRIMARY_OWNER;
import static org.mockito.Mockito.mock;

import fixtures.core.model.AuditInfoFixtures;
import inmemory.ApplicationCrudServiceInMemory;
import inmemory.ApplicationMetadataCrudServiceInMemory;
import inmemory.ApplicationMetadataQueryServiceInMemory;
import inmemory.CRDMembersDomainServiceInMemory;
import inmemory.GroupQueryServiceInMemory;
import inmemory.ImportApplicationCRDDomainServiceInMemory;
import inmemory.RoleQueryServiceInMemory;
import inmemory.UserDomainServiceInMemory;
import io.gravitee.apim.core.application.domain_service.ValidateApplicationCRDDomainService;
import io.gravitee.apim.core.application.model.crd.ApplicationCRDSpec;
import io.gravitee.apim.core.application.model.crd.ApplicationMetadataCRD;
import io.gravitee.apim.core.application.use_case.ImportApplicationCRDUseCase;
import io.gravitee.apim.core.audit.model.AuditInfo;
import io.gravitee.apim.core.group.domain_service.ValidateGroupsDomainService;
import io.gravitee.apim.core.member.domain_service.ValidateCRDMembersDomainService;
import io.gravitee.apim.core.member.model.crd.MemberCRD;
import io.gravitee.apim.core.membership.model.Role;
import io.gravitee.apim.core.user.model.BaseUserEntity;
import io.gravitee.apim.infra.domain_service.application.ValidateApplicationSettingsDomainServiceImpl;
import io.gravitee.common.utils.TimeProvider;
import io.gravitee.definition.model.Origin;
import io.gravitee.repository.management.api.ApplicationRepository;
import io.gravitee.rest.api.model.ApplicationMetadataEntity;
import io.gravitee.rest.api.model.BaseApplicationEntity;
import io.gravitee.rest.api.model.MetadataFormat;
import io.gravitee.rest.api.model.application.ApplicationSettings;
import io.gravitee.rest.api.model.application.SimpleApplicationSettings;
import io.gravitee.rest.api.service.ParameterService;
import io.gravitee.rest.api.service.common.UuidString;
import io.gravitee.rest.api.service.configuration.application.ApplicationTypeService;
import io.gravitee.rest.api.service.impl.configuration.application.ApplicationTypeServiceImpl;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.util.Date;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import org.assertj.core.api.SoftAssertions;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

/**
 * @author Kamiel Ahmadpour (kamiel.ahmadpour at graviteesource.com)
 * @author GraviteeSource Team
 */
public class ImportApplicationCRDUseCaseTest {

    private static final Instant INSTANT_NOW = Instant.parse("2023-10-22T10:15:30Z");
    private static final String ORGANIZATION_ID = "organization-id";
    private static final String ENVIRONMENT_ID = "environment-id";
    private static final String USER_ID = "user-id";
    private static final String ACTOR_USER_ID = "actor-user-id";
    private static final String APP_ID = UuidString.generateRandom();
    private static final String APP_NAME = "test_app";
    private static final String APP_DESCRIPTION = "test_app_description";
    private static final String APP_TYPE = "Linux";
    private static final String TEST_METADATA_NAME = "test_metadata";
    private static final String TEST_METADATA_DEFAULT_VALUE = "test_metadata_default_value";
    private static final String TEST_METADATA_VALUE = "test_metadata_value";
    private static final Date NOW = new Date();
    private static final AuditInfo AUDIT_INFO = AuditInfoFixtures.anAuditInfo(ORGANIZATION_ID, ENVIRONMENT_ID, ACTOR_USER_ID);
    private static final String MEMBER_SOURCE = "test_source";
    private static final String MEMBER_SOURCE_ID_1 = "mem1@email.com";
    private static final String MEMBER_SOURCE_ID_2 = "mem2@email.com";

    private final ApplicationCrudServiceInMemory applicationCrudService = new ApplicationCrudServiceInMemory();
    private final ImportApplicationCRDDomainServiceInMemory importApplicationCRDDomainService =
        new ImportApplicationCRDDomainServiceInMemory();
    private final ApplicationMetadataCrudServiceInMemory applicationMetadataCrudService = new ApplicationMetadataCrudServiceInMemory();
    private final ApplicationMetadataQueryServiceInMemory applicationMetadataQueryService = new ApplicationMetadataQueryServiceInMemory();
    private final UserDomainServiceInMemory userDomainService = new UserDomainServiceInMemory();
    private final RoleQueryServiceInMemory roleQueryService = new RoleQueryServiceInMemory();
    private final CRDMembersDomainServiceInMemory membersDomainService = new CRDMembersDomainServiceInMemory();
    private final ApplicationRepository applicationRepository = mock(ApplicationRepository.class);
    private final ParameterService parameterService = mock(ParameterService.class);

    private final GroupQueryServiceInMemory groupQueryService = new GroupQueryServiceInMemory();
    // Validation
    private final ValidateApplicationCRDDomainService crdValidator = new ValidateApplicationCRDDomainService(
        new ValidateGroupsDomainService(groupQueryService),
        new ValidateCRDMembersDomainService(userDomainService, roleQueryService),
        new ValidateApplicationSettingsDomainServiceImpl(applicationRepository, new ApplicationTypeServiceImpl(), parameterService)
    );

    ImportApplicationCRDUseCase useCase;

    @BeforeEach
    void setUp() {
        importApplicationCRDDomainService.initWith(List.of(anApplicationCRD()));
        userDomainService.initWith(getUsers());
        useCase =
            new ImportApplicationCRDUseCase(
                applicationCrudService,
                importApplicationCRDDomainService,
                applicationMetadataCrudService,
                applicationMetadataQueryService,
                membersDomainService,
                crdValidator
            );
        roleQueryService.initWith(
            List.of(
                Role
                    .builder()
                    .name(PRIMARY_OWNER.name())
                    .referenceType(Role.ReferenceType.ORGANIZATION)
                    .referenceId(ORGANIZATION_ID)
                    .id("primary_owner_id")
                    .scope(Role.Scope.APPLICATION)
                    .build(),
                Role
                    .builder()
                    .name("USER")
                    .referenceType(Role.ReferenceType.ORGANIZATION)
                    .referenceId(ORGANIZATION_ID)
                    .id("user_role_id")
                    .scope(Role.Scope.APPLICATION)
                    .build(),
                Role
                    .builder()
                    .name("OWNER")
                    .referenceType(Role.ReferenceType.ORGANIZATION)
                    .referenceId(ORGANIZATION_ID)
                    .id("user_role_id")
                    .scope(Role.Scope.APPLICATION)
                    .build()
            )
        );
    }

    @BeforeAll
    static void beforeAll() {
        UuidString.overrideGenerator(() -> "generated-id");
        TimeProvider.overrideClock(Clock.fixed(INSTANT_NOW, ZoneId.systemDefault()));
    }

    @AfterAll
    static void afterAll() {
        UuidString.reset();
        TimeProvider.overrideClock(Clock.systemDefaultZone());
    }

    @Nested
    class Create {

        @Test
        void should_create_new_application() {
            var expected = expectedApplication();
            ApplicationCRDSpec crd = anApplicationCRD();
            crd.setId(null);
            useCase.execute(new ImportApplicationCRDUseCase.Input(AUDIT_INFO, crd));

            SoftAssertions.assertSoftly(soft -> soft.assertThat(importApplicationCRDDomainService.storage()).contains(expected));
        }

        @Test
        void should_create_new_application_and_its_metadata() {
            var expectedApp = expectedApplication();
            ApplicationCRDSpec crd = anApplicationCRD();
            crd.setId(null);
            useCase.execute(new ImportApplicationCRDUseCase.Input(AUDIT_INFO, crd));

            SoftAssertions.assertSoftly(soft -> {
                soft.assertThat(importApplicationCRDDomainService.storage()).contains(expectedApp);
                soft.assertThat(applicationMetadataCrudService.storage()).contains(expectedApplicationMetadata());
            });
        }

        @Test
        void should_create_new_application_and_its_members() {
            var expectedApp = expectedApplication();
            var expectedMembers = applicationMembers()
                .stream()
                .map(member -> member.toBuilder().id(USER_ID).build())
                .collect(Collectors.toSet());

            var crd = anApplicationCRD();

            crd.setMembers(applicationMembers());

            useCase.execute(new ImportApplicationCRDUseCase.Input(AUDIT_INFO, crd));

            SoftAssertions.assertSoftly(soft -> {
                soft.assertThat(importApplicationCRDDomainService.storage()).contains(expectedApp);
                soft.assertThat(Set.copyOf(membersDomainService.getApplicationMembers(APP_ID))).isEqualTo(expectedMembers);
            });
        }
    }

    @Nested
    class Update {

        @BeforeEach
        void setUp() {
            applicationCrudService.initWith(List.of(anApplicationCRD()));
        }

        @Test
        void should_update_existing_application() {
            String appDescriptionUpdated = "new_app_description";

            ApplicationCRDSpec crd = anApplicationCRD();
            crd.setDescription(appDescriptionUpdated);
            useCase.execute(new ImportApplicationCRDUseCase.Input(AUDIT_INFO, crd));

            var expected = expectedApplication();
            expected.setDescription(appDescriptionUpdated);
            SoftAssertions.assertSoftly(soft -> soft.assertThat(importApplicationCRDDomainService.storage()).contains(expected));
        }

        @Test
        void should_update_existing_application_and_its_metadata() {
            var expectedApp = expectedApplication();
            ApplicationCRDSpec crd = anApplicationCRD();
            ApplicationMetadataCRD applicationMetadataCRD = crd.getMetadata().get(0);
            String newAppMetadataDescription = "new_app_metadata_description";
            applicationMetadataCRD.setValue(newAppMetadataDescription);

            useCase.execute(new ImportApplicationCRDUseCase.Input(AUDIT_INFO, crd));

            SoftAssertions.assertSoftly(soft -> {
                soft.assertThat(importApplicationCRDDomainService.storage()).contains(expectedApp);

                ApplicationMetadataEntity applicationMetadataEntity = expectedApplicationMetadata();
                applicationMetadataEntity.setValue(newAppMetadataDescription);
                soft.assertThat(applicationMetadataCrudService.storage()).contains(applicationMetadataEntity);
            });
        }

        @Test
        void should_update_existing_application_and_its_members() {
            var expectedApp = expectedApplication();
            var expectedMembers = applicationMembers()
                .stream()
                .map(member -> member.toBuilder().id(USER_ID).build())
                .collect(Collectors.toSet());

            var crd = anApplicationCRD();

            crd.setMembers(applicationMembers());

            useCase.execute(new ImportApplicationCRDUseCase.Input(AUDIT_INFO, crd));

            SoftAssertions.assertSoftly(soft -> {
                soft.assertThat(importApplicationCRDDomainService.storage()).contains(expectedApp);
                soft.assertThat(Set.copyOf(membersDomainService.getApplicationMembers(APP_ID))).isEqualTo(expectedMembers);
            });
        }
    }

    private static ApplicationCRDSpec anApplicationCRD() {
        return ApplicationCRDSpec
            .builder()
            .id(APP_ID)
            .name(APP_NAME)
            .description(APP_DESCRIPTION)
            .type(APP_TYPE)
            .createdAt(NOW)
            .updatedAt(NOW)
            .settings(new ApplicationSettings(new SimpleApplicationSettings(APP_TYPE, "junit"), null))
            .metadata(List.of(anApplicationMetadata()))
            .build();
    }

    private static ApplicationMetadataCRD anApplicationMetadata() {
        return ApplicationMetadataCRD
            .builder()
            .name(TEST_METADATA_NAME)
            .defaultValue(TEST_METADATA_DEFAULT_VALUE)
            .format(MetadataFormat.STRING)
            .value(TEST_METADATA_VALUE)
            .build();
    }

    private Set<MemberCRD> applicationMembers() {
        return Set.of(
            MemberCRD.builder().source(MEMBER_SOURCE).sourceId(MEMBER_SOURCE_ID_2).role("USER").build(),
            MemberCRD.builder().source(MEMBER_SOURCE).sourceId(MEMBER_SOURCE_ID_1).role("OWNER").build()
        );
    }

    private BaseApplicationEntity expectedApplication() {
        var bae = new BaseApplicationEntity();
        bae.setId(APP_ID);
        bae.setName(APP_NAME);
        bae.setDescription(APP_DESCRIPTION);
        bae.setType(APP_TYPE);
        bae.setCreatedAt(NOW);
        bae.setUpdatedAt(NOW);
        bae.setEnvironmentId(ENVIRONMENT_ID);
        bae.setOrigin(Origin.KUBERNETES);

        return bae;
    }

    private ApplicationMetadataEntity expectedApplicationMetadata() {
        var am = new ApplicationMetadataEntity();
        am.setApplicationId(APP_ID);
        am.setName(APP_NAME);
        am.setFormat(MetadataFormat.STRING);
        am.setValue(TEST_METADATA_VALUE);

        return am;
    }

    private List<BaseUserEntity> getUsers() {
        return List.of(
            new BaseUserEntity(
                USER_ID,
                ORGANIZATION_ID,
                "test1",
                "test1",
                MEMBER_SOURCE_ID_1,
                new Date(),
                new Date(),
                MEMBER_SOURCE,
                MEMBER_SOURCE_ID_1
            ),
            new BaseUserEntity(
                USER_ID,
                ORGANIZATION_ID,
                "test2",
                "test2",
                MEMBER_SOURCE_ID_2,
                new Date(),
                new Date(),
                MEMBER_SOURCE,
                MEMBER_SOURCE_ID_2
            )
        );
    }
}
