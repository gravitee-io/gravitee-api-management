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
package io.gravitee.definition.jackson.services.dynamicproperty;

import com.fasterxml.jackson.databind.JsonMappingException;
import io.gravitee.definition.jackson.AbstractTest;
import io.gravitee.definition.model.Api;
import io.gravitee.definition.model.services.dynamicproperty.DynamicPropertyProviderConfiguration;
import io.gravitee.definition.model.services.dynamicproperty.DynamicPropertyService;
import io.gravitee.definition.model.services.dynamicproperty.http.HttpDynamicPropertyProviderConfiguration;
import java.util.concurrent.TimeUnit;
import org.junit.Assert;
import org.junit.Test;

/**
 * @author David BRASSELY (david.brassely at graviteesource.com)
 * @author GraviteeSource Team
 */
public class DynamicPropertyServiceDeserializerTest extends AbstractTest {

    @Test
    public void definition_withDynamicProperty() throws Exception {
        Api api = load("/io/gravitee/definition/jackson/services/dynamicproperty/api-withservice-dynamicproperty.json", Api.class);
        DynamicPropertyService dynamicPropertyService = api.getService(DynamicPropertyService.class);

        Assert.assertNotNull(dynamicPropertyService);

        // Check service configuration
        Assert.assertTrue(dynamicPropertyService.isEnabled());

        // Check scheduling configuration
        Assert.assertEquals(60, dynamicPropertyService.getTrigger().getRate());
        Assert.assertEquals(TimeUnit.SECONDS, dynamicPropertyService.getTrigger().getUnit());

        // Check provider
        Assert.assertNotNull(dynamicPropertyService.getProvider());

        // Check configuration
        DynamicPropertyProviderConfiguration configuration = dynamicPropertyService.getConfiguration();
        Assert.assertNotNull(configuration);

        Assert.assertEquals("http://my_configuration_url", ((HttpDynamicPropertyProviderConfiguration) configuration).getUrl());
        Assert.assertEquals("{}", ((HttpDynamicPropertyProviderConfiguration) configuration).getSpecification());
    }

    @Test(expected = JsonMappingException.class)
    public void definition_withDynamicProperty_badUnit() throws Exception {
        Api api = load("/io/gravitee/definition/jackson/services/dynamicproperty/api-withservice-dynamicproperty-badUnit.json", Api.class);

        Assert.assertNotNull(api);
    }

    @Test(expected = JsonMappingException.class)
    public void definition_withDynamicProperty_noProvider() throws Exception {
        Api api = load("/io/gravitee/definition/jackson/services/dynamicproperty/api-withservice-dynamicproperty-badUnit.json", Api.class);

        Assert.assertNotNull(api);
    }

    @Test
    public void definition_withDynamicProperty_unitInLowerCase() throws Exception {
        Api api = load(
            "/io/gravitee/definition/jackson/services/dynamicproperty/api-withservice-dynamicproperty-unitInLowerCase.json",
            Api.class
        );
        DynamicPropertyService dynamicPropertyService = api.getService(DynamicPropertyService.class);
        Assert.assertNotNull(dynamicPropertyService);
        Assert.assertTrue(dynamicPropertyService.isEnabled());
        Assert.assertEquals(60, dynamicPropertyService.getTrigger().getRate());
        Assert.assertEquals(TimeUnit.SECONDS, dynamicPropertyService.getTrigger().getUnit());
    }

    @Test(expected = JsonMappingException.class)
    public void definition_withDynamicProperty_httpProvider_noUrl() throws Exception {
        Api api = load("/io/gravitee/definition/jackson/services/dynamicproperty/api-withservice-dynamicproperty-noUrl.json", Api.class);
        api.getServices().get(DynamicPropertyService.class);
    }

    @Test(expected = JsonMappingException.class)
    public void definition_withDynamicProperty_httpProvider_noSpecification() throws Exception {
        Api api = load(
            "/io/gravitee/definition/jackson/services/dynamicproperty/api-withservice-dynamicproperty-noSpecification.json",
            Api.class
        );
        api.getServices().get(DynamicPropertyService.class);
    }
}
