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

import static org.mockito.Mockito.*;

import io.gravitee.rest.api.service.Upgrader;
import java.util.Map;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InOrder;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.context.ApplicationContext;

@RunWith(MockitoJUnitRunner.class)
public class UpgraderServiceImplTest {

    @InjectMocks
    private UpgraderServiceImpl upgraderService;

    @Mock
    private ApplicationContext applicationContext;

    @Test
    public void should_call_each_upgrader_with_order() throws Exception {
        Upgrader upgrader1 = mock(Upgrader.class);
        when(upgrader1.getOrder()).thenReturn(150);

        Upgrader upgrader2 = mock(Upgrader.class);
        when(upgrader2.getOrder()).thenReturn(149);

        Upgrader upgrader3 = mock(Upgrader.class);
        when(upgrader3.getOrder()).thenReturn(300);

        when(applicationContext.getBeansOfType(Upgrader.class)).thenReturn(Map.of("1", upgrader1, "2", upgrader2, "3", upgrader3));

        upgraderService.start();

        InOrder inOrder = inOrder(upgrader1, upgrader2, upgrader3);
        inOrder.verify(upgrader2, times(1)).upgrade();
        inOrder.verify(upgrader1, times(1)).upgrade();
        inOrder.verify(upgrader3, times(1)).upgrade();
    }
}
