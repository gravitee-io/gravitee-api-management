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
package io.gravitee.definition.jackson.plugins.resources;

import io.gravitee.definition.jackson.AbstractTest;
import io.gravitee.definition.model.Api;
import org.junit.Assert;
import org.junit.Test;

/**
 * @author David BRASSELY (david.brassely at graviteesource.com)
 * @author GraviteeSource Team
 */
public class ResourceSerializerTest extends AbstractTest {

    @Test
    public void emptyResource() throws Exception {
        Api api = load("/io/gravitee/definition/jackson/api-defaultpath.json", Api.class);

        String generatedJsonDefinition = objectMapper().writeValueAsString(api);
        Assert.assertNotNull(generatedJsonDefinition);
    }

    @Test
    public void withResource() throws Exception {
        Api api = load("/io/gravitee/definition/jackson/api-withresource.json", Api.class);

        String generatedJsonDefinition = objectMapper().writeValueAsString(api);
        Assert.assertNotNull(generatedJsonDefinition);
    }

    @Test
    public void withMultipleResources() throws Exception {
        Api api = load("/io/gravitee/definition/jackson/api-withresources.json", Api.class);

        String generatedJsonDefinition = objectMapper().writeValueAsString(api);
        Assert.assertNotNull(generatedJsonDefinition);
    }

    @Test
    public void withDuplicatedResources() throws Exception {
        Api api = load("/io/gravitee/definition/jackson/api-withduplicatedresources.json", Api.class);

        String generatedJsonDefinition = objectMapper().writeValueAsString(api);
        Assert.assertNotNull(generatedJsonDefinition);
    }
}
