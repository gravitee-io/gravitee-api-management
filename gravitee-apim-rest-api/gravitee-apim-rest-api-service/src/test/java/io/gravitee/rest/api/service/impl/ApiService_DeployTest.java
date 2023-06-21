/**
 * Copyright (C) 2015 The Gravitee team (http://gravitee.io)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.gravitee.rest.api.service.impl;

import static io.gravitee.definition.model.DefinitionContext.ORIGIN_KUBERNETES;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;

import io.gravitee.repository.exceptions.TechnicalException;
import io.gravitee.repository.management.api.ApiRepository;
import io.gravitee.repository.management.model.Api;
import io.gravitee.rest.api.model.*;
import io.gravitee.rest.api.model.api.ApiDeploymentEntity;
import io.gravitee.rest.api.model.api.ApiEntity;
import io.gravitee.rest.api.service.ApiService;
import io.gravitee.rest.api.service.CategoryService;
import io.gravitee.rest.api.service.EventService;
import io.gravitee.rest.api.service.common.GraviteeContext;
import io.gravitee.rest.api.service.converter.ApiConverter;
import io.gravitee.rest.api.service.v4.PrimaryOwnerService;
import java.util.*;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnitRunner;

/**
 * @author GraviteeSource Team
 */
@RunWith(MockitoJUnitRunner.class)
public class ApiService_DeployTest {

    @Mock
    ApiRepository apiRepository;

    @Mock
    EventService eventService;

    @Mock
    private PrimaryOwnerService primaryOwnerService;

    @Mock
    private CategoryService categoryService;

    @Mock
    private ApiConverter apiConverter;

    @InjectMocks
    ApiService apiService = new ApiServiceImpl();

    @Test
    public void shouldDeployIfManagedByKubernetes() throws TechnicalException {
        final String apiId = "kubernetes-api";
        final Api api = new Api();
        api.setId(apiId);
        api.setOrigin(ORIGIN_KUBERNETES);
        Mockito.when(apiRepository.findById(apiId)).thenReturn(Optional.of(api));
        Mockito.when(apiRepository.update(any())).thenReturn(api);
        Mockito.when(eventService.createApiEvent(any(), any(), any(), any(), any())).thenReturn(new EventEntity());

        Map<String, PrimaryOwnerEntity> primaryOwnerEntity = new HashMap<>();
        primaryOwnerEntity.put(apiId, new PrimaryOwnerEntity(new UserEntity()));
        when(primaryOwnerService.getPrimaryOwners(any(), any())).thenReturn(primaryOwnerEntity);

        CategoryEntity category1 = new CategoryEntity();
        category1.setId("cat1");
        category1.setKey("category1");
        when(categoryService.findAll("DEFAULT")).thenReturn(List.of(category1));
        when(apiConverter.toApiEntity(any(), any(), any(), any(), anyBoolean())).thenReturn(new ApiEntity());

        apiService.deploy(GraviteeContext.getExecutionContext(), apiId, "user", EventType.STOP_API, new ApiDeploymentEntity());
    }
}
