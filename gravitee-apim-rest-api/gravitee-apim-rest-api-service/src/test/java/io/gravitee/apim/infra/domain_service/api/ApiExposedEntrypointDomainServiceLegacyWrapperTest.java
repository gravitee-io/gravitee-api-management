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

import static org.mockito.Mockito.verify;

import fixtures.core.model.ApiFixtures;
import io.gravitee.apim.infra.adapter.ApiAdapter;
import io.gravitee.rest.api.service.common.ExecutionContext;
import io.gravitee.rest.api.service.v4.ApiEntrypointService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ApiExposedEntrypointDomainServiceLegacyWrapperTest {

    private static final String ENVIRONMENT_ID = "environment-id";
    private static final String ORGANIZATION_ID = "organization-id";

    @Mock
    ApiEntrypointService apiEntrypointService;

    ApiExposedEntrypointDomainServiceLegacyWrapper service;

    @BeforeEach
    void setUp() {
        service = new ApiExposedEntrypointDomainServiceLegacyWrapper(apiEntrypointService);
    }

    @Test
    void should_call_legacy_service() {
        service.get(ORGANIZATION_ID, ENVIRONMENT_ID, ApiFixtures.aProxyApiV4());

        verify(apiEntrypointService).getApiEntrypoints(
            new ExecutionContext(ORGANIZATION_ID, ENVIRONMENT_ID),
            ApiAdapter.INSTANCE.toApiEntity(ApiFixtures.aProxyApiV4())
        );
    }
}
