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
package io.gravitee.apim.infra.domain_service.membership;

import static fixtures.core.model.MembershipFixtures.anApplicationMembership;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import io.gravitee.apim.core.application.crud_service.ApplicationCrudService;
import io.gravitee.apim.core.exception.TechnicalDomainException;
import io.gravitee.apim.core.membership.model.Membership;
import io.gravitee.apim.core.membership.query_service.MembershipQueryService;
import io.gravitee.rest.api.model.BaseApplicationEntity;
import io.gravitee.rest.api.service.exceptions.ApplicationNotFoundException;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class SearchApplicationMembersDomainServiceImplTest {

    private static final String ENVIRONMENT_ID = "env-id";
    private static final String APPLICATION_ID = "app-id";

    MembershipQueryService membershipQueryService;
    ApplicationCrudService applicationCrudService;

    SearchApplicationMembersDomainServiceImpl service;

    @BeforeEach
    void setUp() {
        membershipQueryService = mock(MembershipQueryService.class);
        applicationCrudService = mock(ApplicationCrudService.class);
        service = new SearchApplicationMembersDomainServiceImpl(membershipQueryService, applicationCrudService);
    }

    @Test
    void should_return_memberships_for_application() {
        var membership1 = anApplicationMembership("00000000-0000-0000-0000-000000000001", APPLICATION_ID, "user-1", "role-1");
        var membership2 = anApplicationMembership(
            "00000000-0000-0000-0000-000000000002",
            APPLICATION_ID,
            "user-2",
            "role-2"
        ).withMemberType(Membership.Type.GROUP);

        when(applicationCrudService.findById(APPLICATION_ID, ENVIRONMENT_ID)).thenReturn(
            BaseApplicationEntity.builder().id(APPLICATION_ID).environmentId(ENVIRONMENT_ID).build()
        );
        when(membershipQueryService.findByReference(eq(Membership.ReferenceType.APPLICATION), eq(APPLICATION_ID))).thenReturn(
            Set.of(membership1, membership2)
        );

        var result = service.searchApplicationMembers(ENVIRONMENT_ID, APPLICATION_ID);

        assertThat(result).containsExactlyInAnyOrder(membership1, membership2);
    }

    @Test
    void should_throw_when_query_service_fails() {
        when(applicationCrudService.findById(APPLICATION_ID, ENVIRONMENT_ID)).thenReturn(
            BaseApplicationEntity.builder().id(APPLICATION_ID).environmentId(ENVIRONMENT_ID).build()
        );
        when(membershipQueryService.findByReference(eq(Membership.ReferenceType.APPLICATION), eq(APPLICATION_ID))).thenThrow(
            new TechnicalDomainException("query failed")
        );

        Throwable throwable = catchThrowable(() -> service.searchApplicationMembers(ENVIRONMENT_ID, APPLICATION_ID));

        assertThat(throwable)
            .isInstanceOf(TechnicalDomainException.class)
            .hasMessage("An error occurs while trying to search members for application app-id");
    }

    @Test
    void should_throw_when_application_does_not_exist() {
        when(applicationCrudService.findById(APPLICATION_ID, ENVIRONMENT_ID)).thenThrow(new ApplicationNotFoundException(APPLICATION_ID));

        Throwable throwable = catchThrowable(() -> service.searchApplicationMembers(ENVIRONMENT_ID, APPLICATION_ID));

        assertThat(throwable).isInstanceOf(ApplicationNotFoundException.class);
        verifyNoInteractions(membershipQueryService);
    }
}
