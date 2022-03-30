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
package io.gravitee.definition.jackson.api;

import io.gravitee.definition.jackson.AbstractTest;
import io.gravitee.definition.model.*;
import org.junit.Assert;
import org.junit.Test;

/**
 * @author David BRASSELY (david.brassely at graviteesource.com)
 * @author GraviteeSource Team
 */
public class VirtualHostDeserializerTest extends AbstractTest {

    @Test
    public void test_with_default_path() throws Exception {
        final String virtualHost = "{\"path\" : \"/\"}";

        VirtualHost vHost = objectMapper().readValue(virtualHost, VirtualHost.class);

        Assert.assertNull(vHost.getHost());
        Assert.assertEquals("/", vHost.getPath());
    }

    @Test
    public void test_with_random_path() throws Exception {
        final String virtualHost = "{\"path\" : \"/randomPath\"}";

        VirtualHost vHost = objectMapper().readValue(virtualHost, VirtualHost.class);

        Assert.assertNull(vHost.getHost());
        Assert.assertEquals("/randomPath", vHost.getPath());
    }
}
