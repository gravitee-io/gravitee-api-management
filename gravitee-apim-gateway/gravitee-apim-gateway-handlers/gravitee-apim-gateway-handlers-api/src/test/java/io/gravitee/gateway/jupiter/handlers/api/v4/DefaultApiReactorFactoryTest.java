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
package io.gravitee.gateway.jupiter.handlers.api.v4;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

import io.gravitee.definition.model.DefinitionVersion;
import io.gravitee.definition.model.v4.ApiType;
import io.gravitee.definition.model.v4.listener.http.HttpListener;
import io.gravitee.definition.model.v4.listener.subscription.SubscriptionListener;
import io.gravitee.definition.model.v4.listener.tcp.TcpListener;
import io.gravitee.gateway.reactor.handler.ReactorHandler;
import java.util.Collections;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * @author David BRASSELY (david.brassely at graviteesource.com)
 * @author GraviteeSource Team
 */
@ExtendWith(MockitoExtension.class)
public class DefaultApiReactorFactoryTest {

    private DefaultApiReactorFactory cut;

    @Mock
    private Api api;

    @Mock
    private io.gravitee.definition.model.v4.Api definition;

    @BeforeEach
    public void init() {
        lenient().when(api.getDefinition()).thenReturn(definition);

        cut = new DefaultApiReactorFactory(null, null, null, null, null, null, null, null, null, null, null, null);
    }

    @Test
    public void shouldNotCreateApiWithDefinitionV2() {
        when(api.getDefinitionVersion()).thenReturn(DefinitionVersion.V2);

        boolean create = cut.canCreate(api);
        assertFalse(create);
    }

    @Test
    public void shouldNotCreateApiWithNoListener() {
        when(api.getDefinitionVersion()).thenReturn(DefinitionVersion.V4);
        when(definition.getListeners()).thenReturn(Collections.emptyList());

        boolean create = cut.canCreate(api);
        assertFalse(create);
    }

    @Test
    public void shouldNotCreateApiWithNoHttpOrSubscriptionListener() {
        when(api.getDefinitionVersion()).thenReturn(DefinitionVersion.V4);
        when(definition.getListeners()).thenReturn(Collections.singletonList(new TcpListener()));

        boolean create = cut.canCreate(api);
        assertFalse(create);
    }

    @Test
    public void shouldCreateApiWithHttpListener() {
        when(api.getDefinitionVersion()).thenReturn(DefinitionVersion.V4);
        when(definition.getListeners()).thenReturn(Collections.singletonList(new HttpListener()));

        boolean create = cut.canCreate(api);
        assertTrue(create);
    }

    @Test
    public void shouldCreateApiWithSubscriptionListener() {
        when(api.getDefinitionVersion()).thenReturn(DefinitionVersion.V4);
        when(definition.getListeners()).thenReturn(Collections.singletonList(new SubscriptionListener()));

        boolean create = cut.canCreate(api);
        assertTrue(create);
    }

    @Test
    public void shouldNotCreateApiIfDisabled() {
        when(api.isEnabled()).thenReturn(false);

        ReactorHandler handler = cut.create(api);
        assertNull(handler);
    }
}
