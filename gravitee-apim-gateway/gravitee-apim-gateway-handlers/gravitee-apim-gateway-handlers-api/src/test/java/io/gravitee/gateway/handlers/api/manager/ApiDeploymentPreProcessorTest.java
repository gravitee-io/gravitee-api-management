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
package io.gravitee.gateway.handlers.api.manager;

import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.gravitee.common.util.DataEncryptor;
import io.gravitee.definition.model.Plan;
import io.gravitee.definition.model.Properties;
import io.gravitee.definition.model.Property;
import io.gravitee.gateway.env.GatewayConfiguration;
import io.gravitee.gateway.handlers.api.definition.Api;
import io.gravitee.gateway.reactor.ReactorEvent;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

/**
 * @author Yann TAVERNIER (yann.tavernier at graviteesource.com)
 * @author GraviteeSource Team
 */
@RunWith(MockitoJUnitRunner.class)
public class ApiDeploymentPreProcessorTest {

    @Mock
    private GatewayConfiguration gatewayConfiguration;

    @Mock
    private DataEncryptor dataEncryptor;

    private ApiDeploymentPreProcessor cut;

    @Before
    public void setUp() {
        cut = new ApiDeploymentPreProcessor(dataEncryptor, gatewayConfiguration);
    }

    @Test
    public void shouldNotFilterPlanWithoutMatchingTag() throws Exception {
        final Api api = buildTestApi();

        final Plan mockedPlan = mock(Plan.class);
        when(mockedPlan.getTags()).thenReturn(new HashSet<>());
        api.setPlans(singletonList(mockedPlan));

        cut.prepareApi(api);

        assertTrue(api.getPlans().contains(mockedPlan));
    }

    @Test
    public void shouldFilterPlanWithoutConfiguredTag() throws Exception {
        final Api api = buildTestApi();

        final Plan mockedPlan = mock(Plan.class);
        when(mockedPlan.getTags()).thenReturn(new HashSet<>(List.of("tag")));
        api.setPlans(singletonList(mockedPlan));

        when(gatewayConfiguration.hasMatchingTags(any())).thenReturn(false);

        cut.prepareApi(api);

        assertTrue(api.getPlans().isEmpty());
    }

    @Test
    public void shouldNotFilterPlanWithConfiguredTag() throws Exception {
        final Api api = buildTestApi();

        final Plan mockedPlan = mock(Plan.class);
        when(mockedPlan.getTags()).thenReturn(new HashSet<>(List.of("tag")));
        api.setPlans(singletonList(mockedPlan));

        when(gatewayConfiguration.hasMatchingTags(any())).thenReturn(true);

        cut.prepareApi(api);

        assertTrue(api.getPlans().contains(mockedPlan));
    }

    @Test
    public void shouldDecryptApiPropertiesOnDeployment() throws Exception {
        final Api api = buildTestApi();

        Properties properties = new Properties();
        properties.setProperties(
            List.of(
                new Property("key1", "plain value 1", false),
                new Property("key2", "value2Base64encrypted", true),
                new Property("key3", "value3Base64encrypted", true)
            )
        );
        api.setProperties(properties);

        when(dataEncryptor.decrypt("value2Base64encrypted")).thenReturn("plain value 2");
        when(dataEncryptor.decrypt("value3Base64encrypted")).thenReturn("plain value 3");

        cut.prepareApi(api);

        verify(dataEncryptor, times(2)).decrypt(any());
        assertEquals(Map.of("key1", "plain value 1", "key2", "plain value 2", "key3", "plain value 3"), api.getProperties().getValues());
    }

    private Api buildTestApi() {
        return new ApiBuilder().id("api-test").name("api-name-test").deployedAt(new Date()).build();
    }
}
