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
package io.gravitee.rest.api.services.dynamicproperties;

import static org.mockito.Mockito.*;

import io.gravitee.definition.model.Properties;
import io.gravitee.definition.model.Property;
import io.gravitee.rest.api.model.EventType;
import io.gravitee.rest.api.model.api.ApiEntity;
import io.gravitee.rest.api.service.ApiService;
import io.gravitee.rest.api.service.common.ExecutionContext;
import io.gravitee.rest.api.service.common.GraviteeContext;
import io.gravitee.rest.api.service.converter.ApiConverter;
import io.gravitee.rest.api.service.exceptions.TechnicalManagementException;
import io.gravitee.rest.api.services.dynamicproperties.model.DynamicProperty;
import io.gravitee.rest.api.services.dynamicproperties.provider.Provider;
import io.reactivex.rxjava3.core.Maybe;
import java.util.List;
import java.util.concurrent.Executors;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

/**
 * @author David BRASSELY (david.brassely at graviteesource.com)
 * @author GraviteeSource Team
 */
@RunWith(MockitoJUnitRunner.class)
public class DynamicPropertyUpdaterTest {

    private DynamicPropertyUpdater dynamicPropertyUpdater;
    private final List<DynamicProperty> dynamicProperties = List.of(new DynamicProperty("my-key", "my-value"));
    private final List<Property> propertiesList = List.of(new Property("my-key", "my-value"));
    private final Properties properties = new Properties();

    private ApiEntity existingApi;

    private final ExecutionContext executionContext = new ExecutionContext("DEFAULT", "DEFAULT");

    @Mock
    private Provider provider;

    @Mock
    ApiService apiService;

    @Mock
    ApiConverter apiConverter;

    @Before
    public void setUp() {
        properties.setProperties(propertiesList);

        existingApi = new ApiEntity();
        existingApi.setId("api-id");
        Properties apiProperties = new Properties();
        apiProperties.setProperties(List.of());
        existingApi.setProperties(apiProperties);

        dynamicPropertyUpdater = new DynamicPropertyUpdater(existingApi, Executors.newSingleThreadExecutor(), executionContext);
        when(provider.name()).thenReturn("mock");
        dynamicPropertyUpdater.setProvider(provider);
        dynamicPropertyUpdater.setApiService(apiService);
        dynamicPropertyUpdater.setApiConverter(apiConverter);
    }

    @Test
    public void shouldNotUpdatePropertiesBecauseOfProviderException() {
        when(provider.get()).thenReturn(Maybe.error(new IllegalStateException()));

        dynamicPropertyUpdater.handle().blockingGet();
        verifyNoInteractions(apiService);
    }

    @Test
    public void shouldUpdatePropertiesWithoutDeploymentIfManualChange() {
        when(apiService.findById(eq(executionContext), any())).thenReturn(existingApi);
        // Simulate a manual change
        when(apiService.isSynchronized(eq(executionContext), any())).thenReturn(false);

        when(provider.get()).thenReturn(Maybe.just(dynamicProperties));
        dynamicPropertyUpdater.handle().blockingGet();

        verify(apiService, times(1)).update(eq(executionContext), eq(existingApi.getId()), any(), eq(false), eq(false));
        verify(apiService, never()).deploy(any(), any(), any(), any(), any());
    }

    @Test
    public void shouldUpdatePropertiesAndDeployApi() {
        when(apiService.findById(eq(executionContext), any())).thenReturn(existingApi);
        when(apiService.isSynchronized(eq(executionContext), any())).thenReturn(true);

        when(provider.get()).thenReturn(Maybe.just(dynamicProperties));
        dynamicPropertyUpdater.handle().blockingGet();

        verify(apiService, times(1)).update(eq(executionContext), eq(existingApi.getId()), any(), eq(false), eq(false));
        verify(apiService, times(1))
            .deploy(eq(executionContext), eq(existingApi.getId()), eq("dynamic-property-updater"), eq(EventType.PUBLISH_API), any());
    }

    @Test
    public void shouldNotDeployAPIOnUpdateError() {
        when(apiService.findById(eq(executionContext), any())).thenReturn(existingApi);
        when(apiService.isSynchronized(eq(executionContext), any())).thenReturn(true);

        when(apiService.update(eq(executionContext), eq(existingApi.getId()), any(), eq(false), eq(false)))
            .thenThrow(new TechnicalManagementException("Unable to update the API"));

        when(provider.get()).thenReturn(Maybe.just(dynamicProperties));
        dynamicPropertyUpdater.handle().blockingGet();

        verify(apiService, times(1)).update(any(), any(), any(), eq(false), eq(false));
        verify(apiService, never()).deploy(any(), any(), any(), any(), any());
    }
}
