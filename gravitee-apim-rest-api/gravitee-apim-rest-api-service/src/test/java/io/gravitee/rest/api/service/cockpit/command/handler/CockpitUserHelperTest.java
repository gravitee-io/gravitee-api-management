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
package io.gravitee.rest.api.service.cockpit.command.handler;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import io.gravitee.repository.exceptions.TechnicalException;
import io.gravitee.repository.management.api.UserRepository;
import io.gravitee.repository.management.model.User;
import io.gravitee.rest.api.service.common.ExecutionContext;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
class CockpitUserHelperTest {

    private static final String ORGANIZATION_ID = "org-123";
    private static final String COCKPIT_USER_ID = "cockpit-user-456";
    private static final String APIM_USER_ID = "apim-user-789";

    @Mock
    private UserRepository userRepository;

    @Mock
    private ExecutionContext executionContext;

    @BeforeEach
    void setUp() {
        when(executionContext.getOrganizationId()).thenReturn(ORGANIZATION_ID);
    }

    @Test
    void should_resolve_apim_user_id_when_user_found_in_repository() throws TechnicalException {
        // Given
        User user = User.builder().id(APIM_USER_ID).build();
        when(userRepository.findBySource(CockpitUserHelper.COCKPIT_SOURCE, COCKPIT_USER_ID, ORGANIZATION_ID)).thenReturn(Optional.of(user));

        // When
        String result = CockpitUserHelper.resolveApimUserId(userRepository, executionContext, COCKPIT_USER_ID);

        // Then
        assertThat(result).isEqualTo(APIM_USER_ID);
    }

    @Test
    void should_return_cockpit_user_id_when_user_not_found_in_repository() throws TechnicalException {
        // Given
        when(userRepository.findBySource(CockpitUserHelper.COCKPIT_SOURCE, COCKPIT_USER_ID, ORGANIZATION_ID)).thenReturn(Optional.empty());

        // When
        String result = CockpitUserHelper.resolveApimUserId(userRepository, executionContext, COCKPIT_USER_ID);

        // Then
        assertThat(result).isEqualTo(COCKPIT_USER_ID);
    }

    @Test
    void should_return_cockpit_user_id_when_technical_exception_occurs() throws TechnicalException {
        // Given
        when(userRepository.findBySource(CockpitUserHelper.COCKPIT_SOURCE, COCKPIT_USER_ID, ORGANIZATION_ID)).thenThrow(
            new TechnicalException("Database error")
        );

        // When
        String result = CockpitUserHelper.resolveApimUserId(userRepository, executionContext, COCKPIT_USER_ID);

        // Then
        assertThat(result).isEqualTo(COCKPIT_USER_ID);
    }
}
