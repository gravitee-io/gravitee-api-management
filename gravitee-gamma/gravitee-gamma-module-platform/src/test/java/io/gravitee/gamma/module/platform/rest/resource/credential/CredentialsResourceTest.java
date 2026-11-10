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
package io.gravitee.gamma.module.platform.rest.resource.credential;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.gravitee.apim.core.exception.ValidationDomainException;
import io.gravitee.common.data.domain.Page;
import io.gravitee.gamma.module.platform.core.credential.use_case.SearchCredentialsUseCase;
import io.gravitee.rest.api.idp.api.authentication.UserDetails;
import io.gravitee.rest.api.service.PermissionService;
import io.gravitee.rest.api.service.common.GraviteeContext;
import jakarta.ws.rs.core.SecurityContext;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

// Mockito @InjectMocks fills the resource's @Inject and @Context fields by type, so no Jersey container is needed.
@ExtendWith(MockitoExtension.class)
@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
class CredentialsResourceTest {

    @Mock
    private SecurityContext securityContext;

    @Mock
    private PermissionService permissionService;

    @Mock
    private SearchCredentialsUseCase searchCredentialsUseCase;

    @InjectMocks
    private CredentialsResource resource;

    @BeforeEach
    void setUp() {
        GraviteeContext.setCurrentOrganization("org-1");
        GraviteeContext.setCurrentEnvironment("env-1");
        var user = new UserDetails("user-1", "", List.of());
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(user, null, List.of()));
    }

    @AfterEach
    void tearDown() {
        GraviteeContext.cleanContext();
        SecurityContextHolder.clearContext();
    }

    @Test
    void should_ask_for_the_page_from_0_and_answer_with_the_page_from_1() {
        when(searchCredentialsUseCase.execute(any())).thenReturn(new SearchCredentialsUseCase.Output(new Page<>(List.of(), 1, 0, 25)));

        var response = resource.list("  openai ", 2, 10);

        var input = ArgumentCaptor.forClass(SearchCredentialsUseCase.Input.class);
        verify(searchCredentialsUseCase).execute(input.capture());
        assertThat(input.getValue().pageNumber()).isEqualTo(1);
        assertThat(input.getValue().pageSize()).isEqualTo(10);
        assertThat(input.getValue().query()).isEqualTo("openai");
        assertThat(response.pagination().page()).isEqualTo(2);
        assertThat(response.pagination().pageCount()).isEqualTo(3);
        assertThat(response.pagination().totalCount()).isEqualTo(25);
    }

    @ParameterizedTest
    @CsvSource({ "0,10", "1,0", "1,101" })
    void should_reject_a_page_out_of_range(int page, int perPage) {
        assertThatThrownBy(() -> resource.list(null, page, perPage)).isInstanceOf(ValidationDomainException.class);
    }
}
