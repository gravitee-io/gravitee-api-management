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

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;
import static org.mockito.Mockito.*;

import io.gravitee.repository.exceptions.TechnicalException;
import io.gravitee.repository.management.api.ApiRepository;
import io.gravitee.repository.management.api.search.ApiCriteria;
import io.gravitee.repository.management.api.search.builder.SortableBuilder;
import io.gravitee.repository.management.model.Api;
import io.gravitee.rest.api.model.api.ApiName;
import io.gravitee.rest.api.model.common.Sortable;
import io.gravitee.rest.api.model.common.SortableImpl;
import io.gravitee.rest.api.service.common.GraviteeContext;
import java.util.List;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

/**
 * @author GraviteeSource Team
 */
@RunWith(MockitoJUnitRunner.class)
public class ApiService_FindAllNamesTest {

    private static final String API_1 = "api-1";
    private static final String API_2 = "api-2";
    private static final String API_NAME = "an api";
    private static final String API_NAME_2 = "another api";
    private static final String USER = "a user";
    private static final String ENVIRONMENT_ID = "DEFAULT";
    private static final ApiCriteria.Builder apiCriteriaBuilder = new ApiCriteria.Builder().environmentId(ENVIRONMENT_ID);
    private static final String USER_ROLE_ID = "role-1";

    @InjectMocks
    private ApiServiceImpl apiService;

    @Mock
    private ApiRepository apiRepository;

    @Test
    public void findAllNamesShouldReturnAllNames() throws TechnicalException {
        Api api = new Api();
        api.setId(API_1);
        api.setName(API_NAME);

        ArgumentCaptor<ApiCriteria> criteriaArgumentCaptor = ArgumentCaptor.forClass(ApiCriteria.class);
        when(apiRepository.findAllNames(criteriaArgumentCaptor.capture(), any())).thenReturn(List.of(api));

        List<ApiName> result = apiService.findAllNames(GraviteeContext.getExecutionContext(), USER, null, null, false);

        assertThat(result).hasSize(1).extracting(ApiName::getName, ApiName::getId).containsExactly(tuple(API_NAME, API_1));
        verify(apiRepository).findAllNames(any(ApiCriteria.class), isNull());
        assertThat(criteriaArgumentCaptor.getValue()).usingRecursiveComparison().isEqualTo(apiCriteriaBuilder.build());
    }

    @Test
    public void findAllNamesShouldReturnAllNamesSorted() throws TechnicalException {
        Api api = new Api();
        api.setId(API_1);
        api.setName(API_NAME);

        ArgumentCaptor<ApiCriteria> criteriaArgumentCaptor = ArgumentCaptor.forClass(ApiCriteria.class);
        when(apiRepository.findAllNames(criteriaArgumentCaptor.capture(), any())).thenReturn(List.of(api));
        Sortable sort = new SortableImpl("name", false);
        List<ApiName> result = apiService.findAllNames(GraviteeContext.getExecutionContext(), USER, null, sort, false);

        assertThat(result).hasSize(1).extracting(ApiName::getName, ApiName::getId).containsExactly(tuple(API_NAME, API_1));
        assertThat(criteriaArgumentCaptor.getValue()).usingRecursiveComparison().isEqualTo(apiCriteriaBuilder.build());

        io.gravitee.repository.management.api.search.Sortable expectedSort = new SortableBuilder().field("name").setAsc(false).build();
        verify(apiRepository).findAllNames(any(ApiCriteria.class), eq(expectedSort));
    }
}
