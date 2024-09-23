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
package io.gravitee.rest.api.services.dynamicproperties.provider.http.mapper;

import static org.assertj.core.api.Assertions.assertThat;

import io.gravitee.rest.api.services.dynamicproperties.model.DynamicProperty;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.Collection;
import org.apache.commons.io.IOUtils;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Test;

/**
 * @author GraviteeSource Team
 */
@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
public class JoltMapperTest {

    private JoltMapper mapper;

    @Test
    public void should_return_properties_with_null_specification() throws IOException {
        mapper = new JoltMapper(null);
        String input = read("/jolt/already-formatted.json");

        Collection<DynamicProperty> properties = mapper.map(input);
        assertThat(properties.size()).isEqualTo(10);
        assertThat(properties).contains(new DynamicProperty("stores_id", "1"));
    }

    @Test
    public void should_return_properties_with_empty_specification() throws IOException {
        mapper = new JoltMapper("");
        String input = read("/jolt/already-formatted.json");

        Collection<DynamicProperty> properties = mapper.map(input);
        assertThat(properties.size()).isEqualTo(10);
        assertThat(properties).contains(new DynamicProperty("stores_id", "1"));
    }

    @Test
    public void should_return_properties_with_value_as_key() throws IOException {
        mapper = new JoltMapper(read("/jolt/specification-value-as-key.json"));
        String input = read("/jolt/custom-response.json");

        Collection<DynamicProperty> properties = mapper.map(input);
        assertThat(properties.size()).isEqualTo(10);
        assertThat(properties).contains(new DynamicProperty("1", "stores_id"));
    }

    @Test
    public void should_return_properties_with_specification() throws IOException {
        mapper = new JoltMapper(read("/jolt/specification-key-value-simple.json"));
        String input = read("/jolt/custom-response.json");

        Collection<DynamicProperty> properties = mapper.map(input);
        assertThat(properties.size()).isEqualTo(10);
        assertThat(properties).contains(new DynamicProperty("stores_id", "1"));
    }

    private String read(String resource) throws IOException {
        return IOUtils.toString(this.getClass().getResourceAsStream(resource), Charset.defaultCharset());
    }
}
