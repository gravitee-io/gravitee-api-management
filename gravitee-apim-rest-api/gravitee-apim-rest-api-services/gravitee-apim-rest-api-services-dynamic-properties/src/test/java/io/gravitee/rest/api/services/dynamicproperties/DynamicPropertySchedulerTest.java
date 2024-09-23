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

import static org.mockito.Mockito.any;
import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import io.gravitee.definition.model.Properties;
import io.gravitee.definition.model.Property;
import io.gravitee.rest.api.model.EventType;
import io.gravitee.rest.api.model.api.ApiEntity;
import io.gravitee.rest.api.service.ApiService;
import io.gravitee.rest.api.service.common.ExecutionContext;
import io.gravitee.rest.api.service.converter.ApiConverter;
import io.gravitee.rest.api.service.exceptions.TechnicalManagementException;
import io.gravitee.rest.api.services.dynamicproperties.model.DynamicProperty;
import io.gravitee.rest.api.services.dynamicproperties.provider.Provider;
import io.reactivex.rxjava3.core.Maybe;
import io.reactivex.rxjava3.plugins.RxJavaPlugins;
import io.reactivex.rxjava3.schedulers.TestScheduler;
import java.util.List;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * @author David BRASSELY (david.brassely at graviteesource.com)
 * @author GraviteeSource Team
 */
@ExtendWith(MockitoExtension.class)
@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
public class DynamicPropertySchedulerTest {

    private DynamicPropertyScheduler dynamicPropertyScheduler;
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

    private TestScheduler testScheduler;

    @BeforeEach
    public void setUp() {
        properties.setProperties(propertiesList);

        existingApi = new ApiEntity();
        existingApi.setId("api-id");
        Properties apiProperties = new Properties();
        apiProperties.setProperties(List.of());
        existingApi.setProperties(apiProperties);

        dynamicPropertyScheduler =
            DynamicPropertyScheduler
                .builder()
                .schedule("* * * * * *")
                .apiService(apiService)
                .api(existingApi)
                .executionContext(executionContext)
                .apiConverter(apiConverter)
                .build();
        lenient().when(provider.name()).thenReturn("mock");
        testScheduler = new TestScheduler();
        RxJavaPlugins.setComputationSchedulerHandler(scheduler -> testScheduler);
        RxJavaPlugins.setIoSchedulerHandler(scheduler -> testScheduler);
    }

    @AfterEach
    public void afterEach() {
        dynamicPropertyScheduler.cancel();
        RxJavaPlugins.reset();
    }

    @Test
    public void should_not_update_properties_because_of_provider_exception() {
        when(provider.get()).thenReturn(Maybe.error(new IllegalStateException()));

        dynamicPropertyScheduler.schedule(provider);
        testScheduler.advanceTimeBy(1000, TimeUnit.MILLISECONDS);

        verifyNoInteractions(apiService);
    }

    @Test
    public void should_update_properties_without_deployment_if_manual_change() {
        when(apiService.findById(eq(executionContext), any())).thenReturn(existingApi);
        // Simulate a manual change
        when(apiService.isSynchronized(eq(executionContext), any())).thenReturn(false);

        when(provider.get()).thenReturn(Maybe.just(dynamicProperties));
        dynamicPropertyScheduler.schedule(provider);
        testScheduler.advanceTimeBy(1000, TimeUnit.MILLISECONDS);

        verify(apiService, times(1)).update(eq(executionContext), eq(existingApi.getId()), any(), eq(false), eq(false));
        verify(apiService, never()).deploy(any(), any(), any(), any(), any());
    }

    @Test
    public void should_update_properties_and_deploy_api() {
        when(apiService.findById(eq(executionContext), any())).thenReturn(existingApi);
        when(apiService.isSynchronized(eq(executionContext), any())).thenReturn(true);

        when(provider.get()).thenReturn(Maybe.just(dynamicProperties));
        dynamicPropertyScheduler.schedule(provider);
        testScheduler.advanceTimeBy(1000, TimeUnit.MILLISECONDS);

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
        dynamicPropertyScheduler.schedule(provider);

        verify(apiService, times(1)).update(any(), any(), any(), eq(false), eq(false));
        verify(apiService, never()).deploy(any(), any(), any(), any(), any());
    }
}
