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
package io.gravitee.management.services.dynamicproperties;

import io.gravitee.management.model.ApiEntity;
import io.gravitee.management.services.dynamicproperties.model.DynamicProperty;
import io.gravitee.management.services.dynamicproperties.provider.Provider;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;

import java.util.Collection;
import java.util.Collections;
import java.util.concurrent.CompletableFuture;

/**
 * @author David BRASSELY (david.brassely at graviteesource.com)
 * @author GraviteeSource Team
 */
public class DynamicPropertyUpdaterTest {

    private DynamicPropertyUpdater poller;

    @Mock
    private ApiEntity apiEntity;

    @Mock
    private Provider provider;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        poller = new DynamicPropertyUpdater(apiEntity);
        Mockito.when(provider.name()).thenReturn("mock");
        poller.setProvider(provider);
    }

    @Test
    public void shouldNotUpdatePropertiesBecauseOfProviderException() {
        Mockito.when(provider.get())
                .thenReturn(
                    CompletableFuture
                            .completedFuture((Collection<DynamicProperty>) Collections.<DynamicProperty>emptyList())
                            .whenComplete((dynamicProperties, throwable) -> {
                                throw new IllegalStateException();
                            })
                );

        poller.handle(1L);
    }

    @Test
    public void shouldUpdateProperties() {
        Mockito.when(provider.get())
                .thenReturn(
                        CompletableFuture
                                .supplyAsync(() -> {
                                    DynamicProperty property = new DynamicProperty("my-key", "my-value");
                                    return Collections.singletonList(property);
                                })
                );

        poller.handle(1L);
    }
}
