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
package io.gravitee.definition.model;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * @author GraviteeSource Team
 */
class PropertyTest {

    @Test
    void equals_should_return_true_cause_same_properties() {
        Property property1 = new Property("key", "value", true);
        Property property2 = new Property("key", "value", true);

        Assertions.assertEquals(property1, property2);
    }

    @Test
    void equals_should_return_false_cause_property_has_different_encryption_boolean() {
        Property property1 = new Property("key", "value", true);
        Property property2 = new Property("key", "value", false);

        Assertions.assertNotEquals(property1, property2);
    }

    @Test
    void equals_should_return_false_cause_parameter_is_null() {
        Property property1 = new Property("key", "value", true);

        Assertions.assertNotEquals(null, property1);
    }

    @Test
    void equals_should_return_false_cause_parameter_is_not_a_property() {
        Property property1 = new Property("key", "value", true);

        Assertions.assertNotEquals("a string", property1);
    }

    @Test
    void hashcode_should_return_same_cause_same_properties() {
        Property property1 = new Property("key", "value", true);
        Property property2 = new Property("key", "value", true);

        Assertions.assertEquals(property1.hashCode(), property2.hashCode());
    }

    @Test
    void hashcode_should_return_different_cause_property_has_different_encryption_boolean() {
        Property property1 = new Property("key", "value", true);
        Property property2 = new Property("key", "value", false);

        Assertions.assertNotEquals(property1.hashCode(), property2.hashCode());
    }
}
