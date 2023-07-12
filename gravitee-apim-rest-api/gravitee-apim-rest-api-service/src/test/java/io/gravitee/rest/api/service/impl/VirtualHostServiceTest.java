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
package io.gravitee.rest.api.service.impl;

import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.when;

import io.gravitee.definition.model.VirtualHost;
import io.gravitee.definition.model.v4.listener.http.Path;
import io.gravitee.rest.api.service.VirtualHostService;
import io.gravitee.rest.api.service.common.GraviteeContext;
import io.gravitee.rest.api.service.v4.validation.PathValidationService;
import java.util.Collections;
import java.util.List;
import org.junit.After;
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
public class VirtualHostServiceTest {

    @Mock
    private PathValidationService pathValidationService;

    private VirtualHostService virtualHostService;

    @Before
    public void init() {
        GraviteeContext.setCurrentEnvironment("DEFAULT");
        virtualHostService = new VirtualHostServiceImpl(pathValidationService);
    }

    @After
    public void tearDown() {
        GraviteeContext.cleanContext();
    }

    @Test
    public void shouldKeepOverrideEntrypointValue() {
        VirtualHost vhost = new VirtualHost();
        vhost.setHost("valid.host.gravitee.io");
        vhost.setPath("/validVhostPath");
        vhost.setOverrideEntrypoint(true);

        Path sanitizedPath = new Path();
        sanitizedPath.setHost("valid.host.gravitee.io");
        sanitizedPath.setPath("/validVhostPath/");

        when(pathValidationService.validateAndSanitizePaths(eq(GraviteeContext.getExecutionContext()), isNull(), anyList()))
            .thenReturn(List.of(sanitizedPath));

        List<VirtualHost> virtualHosts = virtualHostService.sanitizeAndValidate(
            GraviteeContext.getExecutionContext(),
            Collections.singletonList(vhost)
        );

        assertEquals(1, virtualHosts.size());
        assertEquals(new VirtualHost("valid.host.gravitee.io", "/validVhostPath/", true), virtualHosts.get(0));
    }
}
