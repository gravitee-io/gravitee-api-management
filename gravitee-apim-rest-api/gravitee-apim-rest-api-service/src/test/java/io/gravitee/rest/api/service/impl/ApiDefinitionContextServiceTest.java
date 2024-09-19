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

import static io.gravitee.definition.model.DefinitionContext.*;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

import io.gravitee.repository.exceptions.TechnicalException;
import io.gravitee.repository.management.api.ApiRepository;
import io.gravitee.repository.management.model.Api;
import io.gravitee.rest.api.model.api.DefinitionContextEntity;
import io.gravitee.rest.api.service.exceptions.ApiNotFoundException;
import io.gravitee.rest.api.service.exceptions.TechnicalManagementException;
import java.util.Optional;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

/**
 * @author GraviteeSource Team
 */
@RunWith(MockitoJUnitRunner.class)
public class ApiDefinitionContextServiceTest {

    private static final String API_ID = "f866a23f-966d-41be-8b79-41126721ab8b";

    @Mock
    private ApiRepository apiRepository;

    @InjectMocks
    private ApiDefinitionContextServiceImpl definitionContextService;

    @Test
    public void shouldSetDefinitionContext() throws TechnicalException {
        Api apiWithNullContext = newApi();
        DefinitionContextEntity kubernetesContext = newKubernetesContext();

        when(apiRepository.findById(API_ID)).thenReturn(Optional.of(apiWithNullContext));

        definitionContextService.setDefinitionContext(API_ID, kubernetesContext);

        verify(apiRepository, times(1))
            .update(
                argThat(api -> {
                    assertThat(api.getOrigin()).isEqualTo(ORIGIN_KUBERNETES);
                    assertThat(api.getMode()).isEqualTo(MODE_FULLY_MANAGED);
                    assertThat(api.getSyncFrom()).isEqualTo(ORIGIN_KUBERNETES);
                    return true;
                })
            );
    }

    @Test
    public void shouldSetSyncFromToManagement() throws TechnicalException {
        Api apiWithNullContext = newApi();
        DefinitionContextEntity kubernetesContext = newKubernetesContext();
        kubernetesContext.setSyncFrom(ORIGIN_MANAGEMENT);

        when(apiRepository.findById(API_ID)).thenReturn(Optional.of(apiWithNullContext));

        definitionContextService.setDefinitionContext(API_ID, kubernetesContext);

        verify(apiRepository, times(1))
            .update(
                argThat(api -> {
                    assertThat(api.getOrigin()).isEqualTo(ORIGIN_KUBERNETES);
                    assertThat(api.getMode()).isEqualTo(MODE_FULLY_MANAGED);
                    assertThat(api.getSyncFrom()).isEqualTo(ORIGIN_MANAGEMENT);
                    return true;
                })
            );
    }

    @Test(expected = ApiNotFoundException.class)
    public void shouldThrowApiNotFoundException() throws TechnicalException {
        when(apiRepository.findById(API_ID)).thenReturn(Optional.empty());
        definitionContextService.setDefinitionContext(API_ID, newKubernetesContext());
    }

    @Test(expected = TechnicalManagementException.class)
    public void shouldThrowTechnicalManagementException() throws TechnicalException {
        when(apiRepository.findById(API_ID)).thenThrow(TechnicalException.class);
        definitionContextService.setDefinitionContext(API_ID, newKubernetesContext());
    }

    private static Api newApi() {
        Api api = new Api();
        api.setId(API_ID);
        return api;
    }

    private static DefinitionContextEntity newKubernetesContext() {
        return new DefinitionContextEntity(ORIGIN_KUBERNETES, MODE_FULLY_MANAGED, ORIGIN_KUBERNETES);
    }
}
