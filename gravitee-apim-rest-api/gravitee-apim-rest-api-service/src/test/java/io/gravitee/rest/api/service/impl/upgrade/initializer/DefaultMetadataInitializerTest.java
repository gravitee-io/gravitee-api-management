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
package io.gravitee.rest.api.service.impl.upgrade.initializer;

import static org.mockito.Mockito.*;

import io.gravitee.rest.api.service.MetadataService;
import io.gravitee.rest.api.service.common.GraviteeContext;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

/**
 * @author GraviteeSource Team
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.WARN)
public class DefaultMetadataInitializerTest {

    @Mock
    private MetadataService metadataService;

    @InjectMocks
    private final DefaultMetadataInitializer initializer = new DefaultMetadataInitializer();

    @Test
    public void shouldSetSupportEmail() {
        initializer.initialize();
        verify(metadataService, times(1)).initialize(eq(GraviteeContext.getExecutionContext()));
    }

    @Test
    public void testOrder() {
        Assertions.assertEquals(InitializerOrder.DEFAULT_METADATA_INITIALIZER, initializer.getOrder());
    }
}
