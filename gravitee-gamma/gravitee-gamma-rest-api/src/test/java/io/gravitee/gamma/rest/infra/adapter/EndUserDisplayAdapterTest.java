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
package io.gravitee.gamma.rest.infra.adapter;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import io.gravitee.apim.core.analytics_engine.domain_service.EndUserDisplaySettingDomainService;
import io.gravitee.apim.core.parameters.model.ParameterContext;
import io.gravitee.apim.core.parameters.query_service.ParametersQueryService;
import io.gravitee.rest.api.model.parameters.Key;
import io.gravitee.rest.api.model.parameters.ParameterReferenceType;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Test;

@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
class EndUserDisplayAdapterTest {

    private final ParametersQueryService parametersQueryService = mock(ParametersQueryService.class);
    private final EndUserDisplayAdapter adapter = new EndUserDisplayAdapter(new EndUserDisplaySettingDomainService(parametersQueryService));

    @Test
    void should_display_the_end_user_where_the_organization_turns_logging_user_displayed_on() {
        when(
            parametersQueryService.findAsBoolean(
                Key.LOGGING_USER_DISPLAYED,
                new ParameterContext(null, "org-displaying", ParameterReferenceType.ORGANIZATION)
            )
        ).thenReturn(true);

        assertThat(adapter.displaysEndUser("org-displaying")).isTrue();
        assertThat(adapter.displaysEndUser("org-hiding")).isFalse();
    }
}
