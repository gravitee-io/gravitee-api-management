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

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.gravitee.definition.model.DefinitionVersion;
import io.gravitee.repository.exceptions.TechnicalException;
import io.gravitee.repository.management.api.ApiRepository;
import io.gravitee.repository.management.api.search.ApiCriteria;
import io.gravitee.repository.management.model.Api;
import io.gravitee.rest.api.service.ApiService;
import io.gravitee.rest.api.service.AuditService;
import io.gravitee.rest.api.service.common.GraviteeContext;
import io.gravitee.rest.api.service.common.UuidString;
import io.gravitee.rest.api.service.converter.ApiConverter;
import io.gravitee.rest.api.service.v4.ApiNotificationService;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class ApiService_DeleteCategoryTest {

    @InjectMocks
    ApiService apiService = new ApiServiceImpl();

    @Mock
    private AuditService auditService;

    @Mock
    private ApiNotificationService apiNotificationService;

    @Mock
    private ApiConverter apiConverter;

    @Spy
    private ApiRepository apiRepository;

    @Test
    public void shouldDeleteApiCategoryReferences() throws TechnicalException {
        final String categoryId = UuidString.generateRandom();

        Api firstOrphan = new Api();
        firstOrphan.setId(UuidString.generateRandom());
        firstOrphan.setCategories(new HashSet<>(Set.of(categoryId)));

        Api secondOrphan = new Api();
        secondOrphan.setId(UuidString.generateRandom());
        secondOrphan.setCategories(new HashSet<>(Set.of(UuidString.generateRandom(), categoryId)));

        when(apiRepository.search(getDefaultApiCriteriaBuilder().category(categoryId).build()))
            .thenReturn(List.of(firstOrphan, secondOrphan));
        apiService.deleteCategoryFromAPIs(GraviteeContext.getExecutionContext(), categoryId);

        verify(apiRepository, times(1)).update(firstOrphan);
        verify(apiRepository, times(1)).update(secondOrphan);

        assertEquals(0, firstOrphan.getCategories().size());
        assertEquals(1, secondOrphan.getCategories().size());
    }

    private ApiCriteria.Builder getDefaultApiCriteriaBuilder() {
        // By default in this service, we do not care for V4 APIs.
        List<DefinitionVersion> allowedDefinitionVersion = new ArrayList<>();
        allowedDefinitionVersion.add(null);
        allowedDefinitionVersion.add(DefinitionVersion.V1);
        allowedDefinitionVersion.add(DefinitionVersion.V2);

        return new ApiCriteria.Builder().definitionVersion(allowedDefinitionVersion);
    }
}
