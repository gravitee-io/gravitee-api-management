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
package io.gravitee.definition.model;

import org.junit.Test;

import java.util.Arrays;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

/**
 * @author Nicolas GERAUD (nicolas.geraud at graviteesource.com) 
 * @author GraviteeSource Team
 */
public class PropertiesTest {

    @Test
    public void shouldSetProperties() {
        Properties props = new Properties();
        props.setProperties(Arrays.asList(
                new Property("key1", "value1"),
                new Property("key2", "value2")));

        assertEquals("not enough properties", 2, props.getProperties().size());
    }

    @Test(expected = RuntimeException.class)
    public void shouldNotSetDuplicateProperties() {
        Properties props = new Properties();
        props.setProperties(Arrays.asList(
                new Property("key1", "value1"),
                new Property("key1", "value2")));

        fail("must not allow duplicate keys !");
    }
}
