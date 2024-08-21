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
package io.gravitee.rest.api.service.impl;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import io.gravitee.repository.exceptions.TechnicalException;
import io.gravitee.repository.management.api.ApiRepository;
import io.gravitee.repository.management.api.ApplicationRepository;
import io.gravitee.repository.management.api.MembershipRepository;
import io.gravitee.rest.api.model.MembershipReferenceType;
import io.gravitee.rest.api.service.MembershipService;
import io.gravitee.rest.api.service.common.GraviteeContext;
import io.gravitee.rest.api.service.exceptions.ApiNotFoundException;
import io.gravitee.rest.api.service.exceptions.ApplicationNotFoundException;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * @author Yann TAVERNIER (yann.tavernier at graviteesource.com)
 * @author GraviteeSource Team
 */
@ExtendWith(MockitoExtension.class)
public class MembershipService_GetUserMemberTest {

    private MembershipService cut;

    @Mock
    private MembershipRepository membershipRepository;

    @Mock
    private ApiRepository apiRepository;

    @Mock
    private ApplicationRepository applicationRepository;

    @BeforeEach
    public void setUp() throws Exception {
        cut =
            new MembershipServiceImpl(
                null,
                null,
                applicationRepository,
                null,
                null,
                null,
                membershipRepository,
                null,
                null,
                null,
                null,
                null,
                apiRepository,
                null,
                null,
                null,
                null
            );
    }

    @Test
    public void should_throw_if_no_api_found() throws TechnicalException {
        when(membershipRepository.findByMemberIdAndMemberTypeAndReferenceTypeAndReferenceId(any(), any(), any(), any()))
            .thenReturn(Set.of());
        when(apiRepository.findById("reference-id")).thenReturn(Optional.empty());

        assertThatThrownBy(() ->
                cut.getUserMember(GraviteeContext.getExecutionContext(), MembershipReferenceType.API, "reference-id", "user-id")
            )
            .isInstanceOf(ApiNotFoundException.class);
    }

    @Test
    public void should_throw_if_no_app_found() throws TechnicalException {
        when(membershipRepository.findByMemberIdAndMemberTypeAndReferenceTypeAndReferenceId(any(), any(), any(), any()))
            .thenReturn(Set.of());
        when(applicationRepository.findById("reference-id")).thenReturn(Optional.empty());

        assertThatThrownBy(() ->
                cut.getUserMember(GraviteeContext.getExecutionContext(), MembershipReferenceType.APPLICATION, "reference-id", "user-id")
            )
            .isInstanceOf(ApplicationNotFoundException.class);
    }
}
