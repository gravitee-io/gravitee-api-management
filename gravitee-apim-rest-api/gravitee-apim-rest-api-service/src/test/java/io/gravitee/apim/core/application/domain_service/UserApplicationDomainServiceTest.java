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
package io.gravitee.apim.core.application.domain_service;

import static fixtures.core.model.MembershipFixtures.anApiMembership;
import static fixtures.core.model.MembershipFixtures.anApplicationMembership;
import static org.assertj.core.api.Assertions.assertThat;

import inmemory.MembershipQueryServiceInMemory;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class UserApplicationDomainServiceTest {

    private static final String USER_ID = "user-1";
    private static final String OTHER_USER_ID = "user-2";

    private final MembershipQueryServiceInMemory membershipQueryService = new MembershipQueryServiceInMemory();

    private UserApplicationDomainService domainService;

    @BeforeEach
    void setUp() {
        domainService = new UserApplicationDomainService(membershipQueryService);
    }

    @AfterEach
    void tearDown() {
        membershipQueryService.reset();
    }

    @Test
    void should_return_application_ids_for_user() {
        membershipQueryService.initWith(List.of(anApplicationMembership(USER_ID, "app-1"), anApplicationMembership(USER_ID, "app-2")));

        var result = domainService.findApplicationIdsByUserId(USER_ID);

        assertThat(result).containsExactlyInAnyOrder("app-1", "app-2");
    }

    @Test
    void should_return_empty_set_when_user_has_no_application_memberships() {
        var result = domainService.findApplicationIdsByUserId(USER_ID);

        assertThat(result).isEmpty();
    }

    @Test
    void should_exclude_application_ids_belonging_to_other_users() {
        membershipQueryService.initWith(
            List.of(anApplicationMembership(USER_ID, "app-1"), anApplicationMembership(OTHER_USER_ID, "app-other"))
        );

        var result = domainService.findApplicationIdsByUserId(USER_ID);

        assertThat(result).containsExactly("app-1");
    }

    @Test
    void should_exclude_non_application_memberships() {
        membershipQueryService.initWith(List.of(anApplicationMembership(USER_ID, "app-1"), anApiMembership("api-1")));

        var result = domainService.findApplicationIdsByUserId(USER_ID);

        assertThat(result).containsExactly("app-1");
    }
}
