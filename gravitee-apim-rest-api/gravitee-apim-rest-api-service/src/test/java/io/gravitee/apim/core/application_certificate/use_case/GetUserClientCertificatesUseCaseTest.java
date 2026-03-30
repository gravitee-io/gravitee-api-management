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
package io.gravitee.apim.core.application_certificate.use_case;

import static fixtures.core.model.ClientCertificateFixtures.aClientCertificate;
import static fixtures.core.model.MembershipFixtures.anApplicationMembership;
import static org.assertj.core.api.Assertions.assertThat;

import inmemory.ApplicationQueryServiceInMemory;
import inmemory.ClientCertificateCrudServiceInMemory;
import inmemory.InMemoryAlternative;
import inmemory.MembershipQueryServiceInMemory;
import io.gravitee.apim.core.application.domain_service.UserApplicationDomainService;
import io.gravitee.rest.api.model.BaseApplicationEntity;
import io.gravitee.rest.api.model.common.PageableImpl;
import java.util.List;
import java.util.stream.Stream;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class GetUserClientCertificatesUseCaseTest {

    private static final String USER_ID = "user-1";
    private static final String ENV_ID = "env-1";

    private final ClientCertificateCrudServiceInMemory clientCertificateCrudService = new ClientCertificateCrudServiceInMemory();
    private final MembershipQueryServiceInMemory membershipQueryService = new MembershipQueryServiceInMemory();
    private final ApplicationQueryServiceInMemory applicationQueryService = new ApplicationQueryServiceInMemory();

    private GetUserClientCertificatesUseCase useCase;

    @BeforeEach
    void setUp() {
        var userApplicationDomainService = new UserApplicationDomainService(membershipQueryService);
        useCase = new GetUserClientCertificatesUseCase(clientCertificateCrudService, userApplicationDomainService, applicationQueryService);
    }

    @AfterEach
    void tearDown() {
        Stream.of(clientCertificateCrudService, membershipQueryService, applicationQueryService).forEach(InMemoryAlternative::reset);
    }

    @Test
    void should_return_certificates_across_user_applications() {
        membershipQueryService.initWith(List.of(anApplicationMembership(USER_ID, "app-1"), anApplicationMembership(USER_ID, "app-2")));
        clientCertificateCrudService.initWith(
            List.of(aClientCertificate("cert-1", "app-1", "fp1"), aClientCertificate("cert-2", "app-2", "fp2"))
        );

        var result = useCase.execute(new GetUserClientCertificatesUseCase.Input(USER_ID, ENV_ID, new PageableImpl(1, 10), false));

        assertThat(result.clientCertificates().getContent()).hasSize(2);
        assertThat(result.clientCertificates().getTotalElements()).isEqualTo(2);
        assertThat(result.applications()).isEmpty();
    }

    @Test
    void should_exclude_certificates_from_other_users_applications() {
        membershipQueryService.initWith(List.of(anApplicationMembership(USER_ID, "app-1")));
        clientCertificateCrudService.initWith(
            List.of(aClientCertificate("cert-1", "app-1", "fp1"), aClientCertificate("cert-other", "app-other", "fp-other"))
        );

        var result = useCase.execute(new GetUserClientCertificatesUseCase.Input(USER_ID, ENV_ID, new PageableImpl(1, 10), false));

        assertThat(result.clientCertificates().getContent()).hasSize(1);
        assertThat(result.clientCertificates().getContent().get(0).applicationId()).isEqualTo("app-1");
    }

    @Test
    void should_return_empty_when_user_has_no_application_memberships() {
        clientCertificateCrudService.initWith(List.of(aClientCertificate("cert-1", "app-1", "fp1")));

        var result = useCase.execute(new GetUserClientCertificatesUseCase.Input(USER_ID, ENV_ID, new PageableImpl(1, 10), false));

        assertThat(result.clientCertificates().getContent()).isEmpty();
        assertThat(result.clientCertificates().getTotalElements()).isZero();
    }

    @Test
    void should_paginate_certificates() {
        membershipQueryService.initWith(List.of(anApplicationMembership(USER_ID, "app-1")));
        clientCertificateCrudService.initWith(
            List.of(
                aClientCertificate("cert-1", "app-1", "fp1"),
                aClientCertificate("cert-2", "app-1", "fp2"),
                aClientCertificate("cert-3", "app-1", "fp3")
            )
        );

        var result = useCase.execute(new GetUserClientCertificatesUseCase.Input(USER_ID, ENV_ID, new PageableImpl(1, 2), false));

        assertThat(result.clientCertificates().getContent()).hasSize(2);
        assertThat(result.clientCertificates().getTotalElements()).isEqualTo(3);
    }

    @Test
    void should_populate_applications_when_includeApplications_is_true() {
        membershipQueryService.initWith(List.of(anApplicationMembership(USER_ID, "app-1")));
        clientCertificateCrudService.initWith(List.of(aClientCertificate("cert-1", "app-1", "fp1")));
        var app = new BaseApplicationEntity();
        app.setId("app-1");
        app.setName("My App");
        app.setEnvironmentId(ENV_ID);
        applicationQueryService.initWith(List.of(app));

        var result = useCase.execute(new GetUserClientCertificatesUseCase.Input(USER_ID, ENV_ID, new PageableImpl(1, 10), true));

        assertThat(result.clientCertificates().getContent()).hasSize(1);
        assertThat(result.applications()).hasSize(1);
        assertThat(result.applications().get(0).getId()).isEqualTo("app-1");
        assertThat(result.applications().get(0).getName()).isEqualTo("My App");
    }

    @Test
    void should_not_populate_applications_when_includeApplications_is_false() {
        membershipQueryService.initWith(List.of(anApplicationMembership(USER_ID, "app-1")));
        clientCertificateCrudService.initWith(List.of(aClientCertificate("cert-1", "app-1", "fp1")));
        var app = new BaseApplicationEntity();
        app.setId("app-1");
        app.setName("My App");
        app.setEnvironmentId(ENV_ID);
        applicationQueryService.initWith(List.of(app));

        var result = useCase.execute(new GetUserClientCertificatesUseCase.Input(USER_ID, ENV_ID, new PageableImpl(1, 10), false));

        assertThat(result.applications()).isEmpty();
    }
}
