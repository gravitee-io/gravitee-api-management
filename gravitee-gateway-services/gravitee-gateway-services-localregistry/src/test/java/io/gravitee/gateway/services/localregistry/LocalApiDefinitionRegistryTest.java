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
package io.gravitee.gateway.services.localregistry;

import io.gravitee.gateway.handlers.api.manager.ApiManager;
import java.net.URL;
import java.net.URLDecoder;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

/**
 * @author David BRASSELY (brasseld at gmail.com)
 */
public class LocalApiDefinitionRegistryTest {

    @Mock
    private ApiManager apiManager;

    private LocalApiDefinitionRegistry registry;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);

        registry = new LocalApiDefinitionRegistry();
        registry.setApiManager(apiManager);
        registry.setEnabled(true);
    }

    @Test
    public void test() throws Exception {
        URL resource = LocalApiDefinitionRegistryTest.class.getResource("/registry");

        registry.setRegistryPath(URLDecoder.decode(resource.getPath(), "UTF-8"));
        registry.start();
    }
}
