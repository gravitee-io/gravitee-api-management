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
package io.gravitee.rest.api.management.v2.rest.mapper;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

import fixtures.PropertyFixtures;
import io.gravitee.definition.model.Properties;
import io.gravitee.rest.api.management.v2.rest.model.Property;
import io.gravitee.rest.api.model.PropertiesEntity;
import io.gravitee.rest.api.model.PropertyEntity;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;

public class PropertiesMapperTest {

    private final PropertiesMapper propertiesMapper = Mappers.getMapper(PropertiesMapper.class);

    @Test
    void shouldMapToEmptyPropertyEntityV4List() {
        var propertyEntityV4List = propertiesMapper.mapToV4List(null);

        assertThat(propertyEntityV4List).isNotNull();
        assertThat(propertyEntityV4List).asList().isEmpty();
    }

    @Test
    void shouldMapToPropertyEntityV4() {
        Property propertyToMap = PropertyFixtures.aProperty();
        var propertyEntityV4List = propertiesMapper.mapToV4List(List.of(propertyToMap));

        assertThat(propertyEntityV4List).isNotNull();
        assertThat(propertyEntityV4List).asList().hasSize(1);

        var propertyEntityV4 = propertyEntityV4List.get(0);
        assertThat(propertyEntityV4).isNotNull();

        assertThat(propertyEntityV4.getKey()).isEqualTo(propertyToMap.getKey());
        assertThat(propertyEntityV4.getValue()).isEqualTo(propertyToMap.getValue());
        assertThat(propertyEntityV4.isDynamic()).isEqualTo(propertyToMap.getDynamic());
        assertThat(propertyEntityV4.isEncryptable()).isFalse();
        assertThat(propertyEntityV4.isEncrypted()).isEqualTo(propertyToMap.getEncrypted());
    }

    @Test
    void shouldMapToEmptyPropertyEntityV2List() {
        var propertyEntityV2List = propertiesMapper.mapToV2List(null);

        assertThat(propertyEntityV2List).isNotNull();
        assertThat(propertyEntityV2List).asList().isEmpty();
    }

    @Test
    void shouldMapToEmptyPropertiesEntityV2() {
        PropertiesEntity propertiesEntity = propertiesMapper.mapToPropertiesV2(null);

        assertThat(propertiesEntity).isNotNull();
        List<PropertyEntity> properties = propertiesEntity.getProperties();
        assertThat(properties).isNotNull();
        assertThat(properties).asList().isEmpty();
    }

    @Test
    void shouldMapToPropertiesEntityV2() {
        Property propertyToMap = PropertyFixtures.aProperty();
        PropertiesEntity propertiesEntity = propertiesMapper.mapToPropertiesV2(List.of(propertyToMap));

        assertThat(propertiesEntity).isNotNull();
        List<PropertyEntity> properties = propertiesEntity.getProperties();
        assertThat(properties).isNotNull();
        assertThat(properties).asList().hasSize(1);

        PropertyEntity convertedProperty = properties.get(0);
        assertThat(convertedProperty).isNotNull();

        assertThat(convertedProperty.getKey()).isEqualTo(propertyToMap.getKey());
        assertThat(convertedProperty.getValue()).isEqualTo(propertyToMap.getValue());
        assertThat(convertedProperty.isDynamic()).isEqualTo(propertyToMap.getDynamic());
        assertThat(convertedProperty.isEncryptable()).isFalse();
        assertThat(convertedProperty.isEncrypted()).isEqualTo(propertyToMap.getEncrypted());
    }

    @Test
    void shouldMapToPropertiesEntityV2_withEncryptable() {
        Property propertyToMap = PropertyFixtures.aProperty().encryptable(true);
        PropertiesEntity propertiesEntity = propertiesMapper.mapToPropertiesV2(List.of(propertyToMap));

        assertThat(propertiesEntity).isNotNull();
        List<PropertyEntity> properties = propertiesEntity.getProperties();
        assertThat(properties).isNotNull();
        assertThat(properties).asList().hasSize(1);

        PropertyEntity convertedProperty = properties.get(0);
        assertThat(convertedProperty).isNotNull();

        assertThat(convertedProperty.getKey()).isEqualTo(propertyToMap.getKey());
        assertThat(convertedProperty.getValue()).isEqualTo(propertyToMap.getValue());
        assertThat(convertedProperty.isDynamic()).isEqualTo(propertyToMap.getDynamic());
        assertThat(convertedProperty.isEncryptable()).isTrue();
        assertThat(convertedProperty.isEncrypted()).isEqualTo(propertyToMap.getEncrypted());
    }

    @Test
    void shouldMapFromPropertiesV2() {
        Properties propertiesToMap = PropertyFixtures.aModelPropertiesV2();
        List<Property> convertedPropertyList = propertiesMapper.map(propertiesToMap);

        assertThat(convertedPropertyList).isNotNull();
        assertThat(convertedPropertyList).asList().hasSize(1);

        Property convertedProperty = convertedPropertyList.get(0);
        assertThat(convertedProperty).isNotNull();

        assertThat(convertedProperty.getKey()).isEqualTo(propertiesToMap.getProperties().get(0).getKey());
        assertThat(convertedProperty.getValue()).isEqualTo(propertiesToMap.getProperties().get(0).getValue());
        assertThat(convertedProperty.getDynamic()).isEqualTo(propertiesToMap.getProperties().get(0).isDynamic());
        assertThat(convertedProperty.getEncrypted()).isEqualTo(propertiesToMap.getProperties().get(0).isEncrypted());
        assertThat(convertedProperty.getEncryptable()).isNull();
    }
}
