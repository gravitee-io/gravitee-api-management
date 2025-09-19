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
import java.util.List;

public class PropertyModelFixtures {

    private PropertyModelFixtures() {}

    private static final io.gravitee.definition.model.Property.PropertyBuilder BASE_MODEL_PROPERTY_V2 =
        io.gravitee.definition.model.Property.builder().dynamic(false).encrypted(false).key("prop-key").value("prop-value");

    private static final io.gravitee.definition.model.v4.property.Property.PropertyBuilder BASE_MODEL_PROPERTY_V4 =
        io.gravitee.definition.model.v4.property.Property.builder().dynamic(false).encrypted(false).key("prop-key").value("prop-value");

    private static final Properties.PropertiesBuilder BASE_MODEL_PROPERTIES_V2 = Properties.builder().propertiesList(
        List.of(BASE_MODEL_PROPERTY_V2.build())
    );

    public static Properties aModelPropertiesV2() {
        return BASE_MODEL_PROPERTIES_V2.build();
    }

    public static io.gravitee.definition.model.v4.property.Property aModelPropertyV4() {
        return BASE_MODEL_PROPERTY_V4.build();
    }
}
