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

import io.gravitee.apim.core.api.model.property.EncryptableProperty;
import io.gravitee.rest.api.management.v2.rest.model.Property;
import io.gravitee.rest.api.model.PropertiesEntity;
import java.util.List;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.NullValueMappingStrategy;
import org.mapstruct.factory.Mappers;

@Mapper(nullValueIterableMappingStrategy = NullValueMappingStrategy.RETURN_DEFAULT)
public interface PropertiesMapper {
    PropertiesMapper INSTANCE = Mappers.getMapper(PropertiesMapper.class);

    // V4
    io.gravitee.rest.api.model.v4.api.properties.PropertyEntity mapToV4(Property property);
    List<io.gravitee.rest.api.model.v4.api.properties.PropertyEntity> mapToV4List(List<Property> property);

    @Mapping(target = "encryptable", ignore = true)
    Property map(io.gravitee.definition.model.v4.property.Property property);

    List<Property> map(List<io.gravitee.definition.model.v4.property.Property> property);

    io.gravitee.definition.model.v4.property.Property mapToV4Definition(Property property);
    List<io.gravitee.definition.model.v4.property.Property> mapToV4Definition(List<Property> property);

    // UpdateNativeApi
    EncryptableProperty mapToEncryptableProperty(Property property);
    List<EncryptableProperty> mapToEncryptableProperties(List<Property> properties);

    // V2
    io.gravitee.rest.api.model.PropertyEntity mapToV2(Property property);
    List<io.gravitee.rest.api.model.PropertyEntity> mapToV2List(List<Property> property);

    @Mapping(target = "encryptable", ignore = true)
    Property map(io.gravitee.definition.model.Property property);

    List<Property> mapToApiModelList(List<io.gravitee.definition.model.Property> properties);

    default PropertiesEntity mapToPropertiesV2(List<Property> propertyList) {
        if (propertyList == null) {
            return new PropertiesEntity();
        }
        return new PropertiesEntity(this.mapToV2List(propertyList));
    }

    default List<Property> map(io.gravitee.definition.model.Properties properties) {
        if (properties == null) {
            return null;
        }
        return this.mapToApiModelList(properties.getProperties());
    }
}
