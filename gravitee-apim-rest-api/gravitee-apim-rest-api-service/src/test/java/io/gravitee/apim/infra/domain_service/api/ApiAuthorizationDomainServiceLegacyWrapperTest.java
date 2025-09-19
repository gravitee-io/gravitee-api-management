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
package io.gravitee.apim.infra.domain_service.api;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

import io.gravitee.apim.core.api.model.ApiQueryCriteria;
import io.gravitee.rest.api.model.api.ApiQuery;
import io.gravitee.rest.api.service.common.ExecutionContext;
import io.gravitee.rest.api.service.v4.ApiAuthorizationService;
import java.util.List;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ApiAuthorizationDomainServiceLegacyWrapperTest {

    private static final String API_ID = "api-id";
    private static final String ENVIRONMENT_ID = "environment-id";
    private static final String ORGANIZATION_ID = "organization-id";

    @Mock
    ApiAuthorizationService apiAuthorizationService;

    ApiAuthorizationDomainServiceLegacyWrapper service;

    @BeforeEach
    void setUp() {
        service = new ApiAuthorizationDomainServiceLegacyWrapper(apiAuthorizationService);
    }

    @Test
    void should_call_legacy_service() {
        service.findIdsByUser(
            new ExecutionContext(ORGANIZATION_ID, ENVIRONMENT_ID),
            "user-id",
            ApiQueryCriteria.builder().ids(List.of(API_ID)).build(),
            null,
            false
        );

        verify(apiAuthorizationService).findIdsByUser(
            new ExecutionContext(ORGANIZATION_ID, ENVIRONMENT_ID),
            "user-id",
            ApiQuery.builder().ids(List.of(API_ID)).build(),
            null,
            false
        );
    }

    @Test
    void should_throw_when_validation_fails() {
        doThrow(new RuntimeException("error")).when(apiAuthorizationService).findIdsByUser(any(), any(), any(), any(), anyBoolean());

        var throwable = Assertions.catchThrowable(() ->
            service.findIdsByUser(
                new ExecutionContext(ORGANIZATION_ID, ENVIRONMENT_ID),
                "user-id",
                ApiQueryCriteria.builder().ids(List.of(API_ID)).build(),
                null,
                false
            )
        );

        Assertions.assertThat(throwable).isInstanceOf(RuntimeException.class);
    }
}
