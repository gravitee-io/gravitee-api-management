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
package io.gravitee.elasticsearch.templating.freemarker;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnitRunner;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

/**
 * Test the template.
 * 
 * @author Guillaume Waignier
 * @author Sebastien Devaux
 *
 */
@RunWith(MockitoJUnitRunner.class)
public class FreeMarkerComponentTest {

    private FreeMarkerComponent freeMarkerComponent = new FreeMarkerComponent();

    @Before
    public void init() throws IOException {
        freeMarkerComponent.afterPropertiesSet();
    }

    @Test
    public void testGenerateFromTemplateWithoutData() {
        Assert.assertNotNull(this.freeMarkerComponent);
        final String result = this.freeMarkerComponent.generateFromTemplate("template.ftl");
        Assert.assertEquals("test", result);
    }

    @Test
    public void testGenerateFromTemplateWithData() {
        final Map<String, Object> data = new HashMap<>();
        data.put("data", "test");

        final String result = this.freeMarkerComponent.generateFromTemplate("templateWithData.ftl", data);
        Assert.assertEquals("test with data : test", result);
    }
}
