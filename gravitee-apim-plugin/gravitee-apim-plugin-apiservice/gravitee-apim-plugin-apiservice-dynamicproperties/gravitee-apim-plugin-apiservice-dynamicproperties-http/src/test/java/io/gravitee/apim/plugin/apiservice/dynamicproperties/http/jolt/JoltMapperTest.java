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
package io.gravitee.apim.plugin.apiservice.dynamicproperties.http.jolt;

import static org.junit.Assert.assertEquals;

import io.gravitee.definition.model.v4.property.Property;
import java.io.IOException;
import java.util.Collection;
import org.junit.Test;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;

/**
 * @author Yann TAVERNIER (yann.tavernier at graviteesource.com)
 * @author GraviteeSource Team
 */
@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
public class JoltMapperTest {

    private JoltMapper mapper;

    private static final String RESPONSE = """
        {
            "content": {
                "name": "Elysee",
                "country": "FRANCE",
                "address": "Avenue des Champs-Élysées",
                "city": "PARIS",
                "stores_id": 1,
                "zip_code": "75000",
                "gps_x": "48.869729",
                "gps_y": "2.307784",
                "phone_number": "01 00 00 00 00",
                "backend_url": "https://north-europe.company.com/"
            }
        }
        """;

    private static final String SPEC_KEY_VALUE_SIMPLE = """
        [
          {
            "operation": "shift",
            "spec": {
              "content": {
                "*": {
                  "$": "[#2].key",
                  "@": "[#2].value"
                }
              }
            }
          }
        ]""";

    private static final String SPEC_VALUE_AS_KEY = """
        [
          {
            "operation": "shift",
            "spec": {
              "content": {
                "*": {
                  "$": "[#2].value",
                  "@": "[#2].key"
                }
              }
            }
          }
        ]""";

    @Test
    public void shouldReturnPropertiesWithValueAsKey() throws IOException {
        mapper = new JoltMapper(SPEC_VALUE_AS_KEY);

        Collection<Property> properties = mapper.map(RESPONSE);
        assertEquals(properties.size(), 10);
        // Should stringify input number value if used as key
        assertEquals(
            properties
                .stream()
                .filter(p -> p.getKey().equals("1"))
                .findFirst()
                .get()
                .getValue(),
            "stores_id"
        );
    }

    @Test
    public void shouldReturnProperties() throws IOException {
        mapper = new JoltMapper(SPEC_KEY_VALUE_SIMPLE);

        Collection<Property> properties = mapper.map(RESPONSE);
        assertEquals(properties.size(), 10);
        // Should stringify input number value if used as value
        assertEquals(
            properties
                .stream()
                .filter(p -> p.getKey().equals("stores_id"))
                .findFirst()
                .get()
                .getValue(),
            "1"
        );
    }
}
