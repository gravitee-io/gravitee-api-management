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
package fixtures;

import io.gravitee.definition.model.Properties;
import io.gravitee.rest.api.management.v2.rest.model.Property;
import java.util.function.Supplier;

public class PropertyFixtures {

    private PropertyFixtures() {}

    private static final Supplier<Property> BASE_PROPERTY = () ->
        new Property().dynamic(false).encrypted(false).key("prop-key").value("prop-value");

    public static Property aProperty() {
        return BASE_PROPERTY.get();
    }

    public static Properties aModelPropertiesV2() {
        return PropertyModelFixtures.aModelPropertiesV2();
    }

    public static io.gravitee.definition.model.v4.property.Property aModelPropertyV4() {
        return PropertyModelFixtures.aModelPropertyV4();
    }
}
