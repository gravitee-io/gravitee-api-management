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
package io.gravitee.apim.core.user.use_case;

import static org.assertj.core.api.Assertions.assertThat;

import inmemory.ApplicationQueryServiceInMemory;
import inmemory.EnvironmentCrudServiceInMemory;
import inmemory.InMemoryAlternative;
import inmemory.MembershipQueryServiceInMemory;
import io.gravitee.apim.core.environment.model.Environment;
import io.gravitee.apim.core.membership.model.Membership;
import io.gravitee.rest.api.model.BaseApplicationEntity;
import java.util.List;
import java.util.stream.Stream;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class GetUserApplicationsUseCaseTest {

    private static final String USER_ID = "user-1";

    MembershipQueryServiceInMemory membershipQueryService = new MembershipQueryServiceInMemory();
    ApplicationQueryServiceInMemory applicationQueryService = new ApplicationQueryServiceInMemory();
    EnvironmentCrudServiceInMemory environmentCrudService = new EnvironmentCrudServiceInMemory();
    GetUserApplicationsUseCase useCase;

    @BeforeEach
    void setUp() {
        useCase = new GetUserApplicationsUseCase(membershipQueryService, applicationQueryService, environmentCrudService);
    }

    @AfterEach
    void tearDown() {
        Stream.of(membershipQueryService, applicationQueryService, environmentCrudService).forEach(InMemoryAlternative::reset);
    }

    @Test
    void should_return_user_applications_with_correct_fields() {
        membershipQueryService.initWith(
            List.of(
                Membership.builder()
                    .id("m1")
                    .memberId(USER_ID)
                    .memberType(Membership.Type.USER)
                    .referenceType(Membership.ReferenceType.APPLICATION)
                    .referenceId("app-1")
                    .build()
            )
        );
        var app = new BaseApplicationEntity();
        app.setId("app-1");
        app.setName("My Application");
        app.setEnvironmentId("env-1");
        applicationQueryService.initWith(List.of(app));
        environmentCrudService.initWith(List.of(Environment.builder().id("env-1").name("Development").build()));

        var output = useCase.execute(new GetUserApplicationsUseCase.Input(USER_ID, null, 1, 10));

        assertThat(output.data()).hasSize(1);
        assertThat(output.totalCount()).isEqualTo(1);

        var result = output.data().get(0);
        assertThat(result.getId()).isEqualTo("app-1");
        assertThat(result.getName()).isEqualTo("My Application");
        assertThat(result.getEnvironmentId()).isEqualTo("env-1");
        assertThat(result.getEnvironmentName()).isEqualTo("Development");
    }

    @Test
    void should_filter_by_environment_id() {
        membershipQueryService.initWith(
            List.of(
                Membership.builder()
                    .id("m1")
                    .memberId(USER_ID)
                    .memberType(Membership.Type.USER)
                    .referenceType(Membership.ReferenceType.APPLICATION)
                    .referenceId("app-1")
                    .build(),
                Membership.builder()
                    .id("m2")
                    .memberId(USER_ID)
                    .memberType(Membership.Type.USER)
                    .referenceType(Membership.ReferenceType.APPLICATION)
                    .referenceId("app-2")
                    .build()
            )
        );
        var app1 = new BaseApplicationEntity();
        app1.setId("app-1");
        app1.setName("App 1");
        app1.setEnvironmentId("env-1");
        var app2 = new BaseApplicationEntity();
        app2.setId("app-2");
        app2.setName("App 2");
        app2.setEnvironmentId("env-2");
        applicationQueryService.initWith(List.of(app1, app2));
        environmentCrudService.initWith(
            List.of(Environment.builder().id("env-1").name("Dev").build(), Environment.builder().id("env-2").name("Prod").build())
        );

        var output = useCase.execute(new GetUserApplicationsUseCase.Input(USER_ID, "env-1", 1, 10));

        assertThat(output.data()).hasSize(1);
        assertThat(output.totalCount()).isEqualTo(1);
        assertThat(output.data().get(0).getId()).isEqualTo("app-1");
    }

    @Test
    void should_return_empty_page_when_user_has_no_memberships() {
        var output = useCase.execute(new GetUserApplicationsUseCase.Input(USER_ID, null, 1, 10));

        assertThat(output.data()).isEmpty();
        assertThat(output.totalCount()).isZero();
    }

    @Test
    void should_paginate_correctly() {
        membershipQueryService.initWith(
            List.of(
                Membership.builder()
                    .id("m1")
                    .memberId(USER_ID)
                    .memberType(Membership.Type.USER)
                    .referenceType(Membership.ReferenceType.APPLICATION)
                    .referenceId("app-1")
                    .build(),
                Membership.builder()
                    .id("m2")
                    .memberId(USER_ID)
                    .memberType(Membership.Type.USER)
                    .referenceType(Membership.ReferenceType.APPLICATION)
                    .referenceId("app-2")
                    .build(),
                Membership.builder()
                    .id("m3")
                    .memberId(USER_ID)
                    .memberType(Membership.Type.USER)
                    .referenceType(Membership.ReferenceType.APPLICATION)
                    .referenceId("app-3")
                    .build()
            )
        );
        var app1 = new BaseApplicationEntity();
        app1.setId("app-1");
        app1.setName("App 1");
        app1.setEnvironmentId("env-1");
        var app2 = new BaseApplicationEntity();
        app2.setId("app-2");
        app2.setName("App 2");
        app2.setEnvironmentId("env-1");
        var app3 = new BaseApplicationEntity();
        app3.setId("app-3");
        app3.setName("App 3");
        app3.setEnvironmentId("env-1");
        applicationQueryService.initWith(List.of(app1, app2, app3));
        environmentCrudService.initWith(List.of(Environment.builder().id("env-1").name("Dev").build()));

        var output = useCase.execute(new GetUserApplicationsUseCase.Input(USER_ID, null, 2, 2));

        assertThat(output.data()).hasSize(1);
        assertThat(output.totalCount()).isEqualTo(3);
        assertThat(output.data().get(0).getId()).isEqualTo("app-3");
    }
}
