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

import static io.gravitee.rest.api.model.parameters.Key.PORTAL_TOP_APIS;
import static java.util.Arrays.asList;
import static java.util.Collections.singletonList;
import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.gravitee.rest.api.model.NewTopApiEntity;
import io.gravitee.rest.api.model.TopApiEntity;
import io.gravitee.rest.api.model.UpdateTopApiEntity;
import io.gravitee.rest.api.model.api.ApiEntity;
import io.gravitee.rest.api.model.parameters.ParameterReferenceType;
import io.gravitee.rest.api.service.ApiService;
import io.gravitee.rest.api.service.ParameterService;
import io.gravitee.rest.api.service.TopApiService;
import io.gravitee.rest.api.service.common.GraviteeContext;
import java.util.List;
import java.util.function.Function;
import java.util.function.Predicate;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

/**
 * @author Azize ELAMRANI (azize at graviteesource.com)
 * @author GraviteeSource Team
 */
@RunWith(MockitoJUnitRunner.class)
public class TopApiServiceTest {

    @InjectMocks
    private TopApiService topApiService = new TopApiServiceImpl();

    @Mock
    private ParameterService parameterService;

    @Mock
    private ApiService apiService;

    @Test
    public void shouldFindAll() {
        final ApiEntity api1 = new ApiEntity();
        api1.setId("1");
        api1.setName("name");
        api1.setVersion("version");
        api1.setDescription("description");
        final ApiEntity api2 = new ApiEntity();
        api2.setId("2");

        when(
            parameterService.findAll(
                eq(GraviteeContext.getExecutionContext()),
                eq(PORTAL_TOP_APIS),
                any(Function.class),
                any(Predicate.class),
                any(ParameterReferenceType.class)
            )
        )
            .thenReturn(asList(api1, api2, api1));

        final List<TopApiEntity> topApis = topApiService.findAll(GraviteeContext.getExecutionContext());

        assertEquals("1", topApis.get(0).getApi());
        assertEquals("name", topApis.get(0).getName());
        assertEquals("version", topApis.get(0).getVersion());
        assertEquals("description", topApis.get(0).getDescription());
        assertEquals(0, topApis.get(0).getOrder());
        assertEquals("2", topApis.get(1).getApi());
        assertEquals(1, topApis.get(1).getOrder());
        assertEquals("1", topApis.get(2).getApi());
        assertEquals(2, topApis.get(2).getOrder());
    }

    @Test
    public void shouldCreate() {
        final NewTopApiEntity topApi = new NewTopApiEntity();
        topApi.setApi("api");

        topApiService.create(GraviteeContext.getExecutionContext(), topApi);

        verify(parameterService)
            .save(GraviteeContext.getExecutionContext(), PORTAL_TOP_APIS, singletonList("api"), ParameterReferenceType.ENVIRONMENT);
        verify(parameterService)
            .findAll(
                eq(GraviteeContext.getExecutionContext()),
                eq(PORTAL_TOP_APIS),
                any(Function.class),
                any(Predicate.class),
                any(ParameterReferenceType.class)
            );
    }

    @Test(expected = IllegalArgumentException.class)
    public void shouldNotCreate() {
        final NewTopApiEntity topApi = new NewTopApiEntity();
        topApi.setApi("api");

        when(parameterService.findAll(GraviteeContext.getExecutionContext(), PORTAL_TOP_APIS, ParameterReferenceType.ENVIRONMENT))
            .thenReturn(singletonList("api"));

        topApiService.create(GraviteeContext.getExecutionContext(), topApi);
    }

    @Test
    public void shouldUpdate() {
        final UpdateTopApiEntity topApi = new UpdateTopApiEntity();
        topApi.setApi("api");
        topApi.setOrder(2);
        final UpdateTopApiEntity topApi2 = new UpdateTopApiEntity();
        topApi2.setApi("api2");
        topApi.setOrder(1);

        when(parameterService.findAll(GraviteeContext.getExecutionContext(), PORTAL_TOP_APIS, ParameterReferenceType.ENVIRONMENT))
            .thenReturn(asList("api", "api2"));

        topApiService.update(GraviteeContext.getExecutionContext(), asList(topApi, topApi2));

        verify(parameterService)
            .save(GraviteeContext.getExecutionContext(), PORTAL_TOP_APIS, asList("api2", "api"), ParameterReferenceType.ENVIRONMENT);
        verify(parameterService)
            .findAll(
                eq(GraviteeContext.getExecutionContext()),
                eq(PORTAL_TOP_APIS),
                any(Function.class),
                any(Predicate.class),
                any(ParameterReferenceType.class)
            );
    }

    @Test(expected = IllegalArgumentException.class)
    public void shouldNotUpdate() {
        final UpdateTopApiEntity topApi = new UpdateTopApiEntity();
        topApi.setApi("api");
        topApi.setOrder(2);
        final UpdateTopApiEntity topApi2 = new UpdateTopApiEntity();
        topApi2.setApi("api2");
        topApi.setOrder(1);

        when(parameterService.findAll(GraviteeContext.getExecutionContext(), PORTAL_TOP_APIS, ParameterReferenceType.ENVIRONMENT))
            .thenReturn(singletonList("api"));

        topApiService.update(GraviteeContext.getExecutionContext(), asList(topApi, topApi2));
    }

    @Test
    public void shouldDelete() {
        final ApiEntity api1 = new ApiEntity();
        api1.setId("1");
        api1.setName("name");
        api1.setVersion("version");
        api1.setDescription("description");
        final ApiEntity api2 = new ApiEntity();
        api2.setId("2");

        when(
            parameterService.findAll(
                eq(GraviteeContext.getExecutionContext()),
                eq(PORTAL_TOP_APIS),
                any(Function.class),
                any(Predicate.class),
                any(ParameterReferenceType.class)
            )
        )
            .thenReturn(asList(api1, api2));

        topApiService.delete(GraviteeContext.getExecutionContext(), "1");

        verify(parameterService)
            .save(GraviteeContext.getExecutionContext(), PORTAL_TOP_APIS, singletonList("2"), ParameterReferenceType.ENVIRONMENT);
    }
}
