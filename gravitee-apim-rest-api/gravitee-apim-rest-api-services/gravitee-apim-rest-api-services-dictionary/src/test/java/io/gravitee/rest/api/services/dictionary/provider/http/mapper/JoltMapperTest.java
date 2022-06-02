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
package io.gravitee.rest.api.services.dictionary.provider.http.mapper;

import static org.junit.Assert.assertEquals;

import io.gravitee.rest.api.services.dictionary.model.DynamicProperty;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.util.Collection;
import org.apache.commons.io.IOUtils;
import org.junit.Test;

/**
 * @author GraviteeSource Team
 */
public class JoltMapperTest {

    private JoltMapper mapper;

    @Test
    public void shouldReturnPropertiesWithValueAsKey() throws IOException {
        mapper = new JoltMapper(read("/jolt/specification-value-as-key.json"));
        String input = IOUtils.toString(read("/jolt/custom-response.json"), Charset.defaultCharset());

        Collection<DynamicProperty> properties = mapper.map(input);
        assertEquals(properties.size(), 10);
        // Should stringify input number value if used as key
        assertEquals(properties.stream().filter(p -> p.getKey().equals("1")).findFirst().get().getValue(), "stores_id");
    }

    @Test
    public void shouldReturnProperties() throws IOException {
        mapper = new JoltMapper(read("/jolt/specification-key-value-simple.json"));
        String input = IOUtils.toString(read("/jolt/custom-response.json"), Charset.defaultCharset());

        Collection<DynamicProperty> properties = mapper.map(input);
        assertEquals(properties.size(), 10);
        // Should stringify input number value if used as value
        assertEquals(properties.stream().filter(p -> p.getKey().equals("stores_id")).findFirst().get().getValue(), "1");
    }

    private InputStream read(String resource) throws IOException {
        return this.getClass().getResourceAsStream(resource);
    }
}
