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
package io.gravitee.rest.api.management.v2.rest.mapper;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

import com.fasterxml.jackson.core.JsonProcessingException;
import fixtures.ResourceFixtures;
import io.gravitee.rest.api.management.v2.rest.model.Resource;
import java.util.LinkedHashMap;
import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;

public class ResourceMapperTest {

    private final ResourceMapper resourceMapper = Mappers.getMapper(ResourceMapper.class);

    @Test
    void shouldMapToResourceEntityV4() {
        Resource resource = ResourceFixtures.aResource();
        var resourceEntityV4 = resourceMapper.mapToV4(resource);

        assertThat(resourceEntityV4).isNotNull();
        assertThat(resourceEntityV4.getName()).isEqualTo(resource.getName());
        assertThat(resourceEntityV4.getType()).isEqualTo(resource.getType());
        assertThat(resourceEntityV4.isEnabled()).isEqualTo(resource.getEnabled());

        // Configuration is tested deeper in ConfigurationSerializationMapperTest
        assertThat(resourceEntityV4.getConfiguration()).isNotNull().isNotEmpty();
    }

    @Test
    void shouldMapToResourceEntityV2() {
        Resource resource = ResourceFixtures.aResource();
        var resourceEntityV2 = resourceMapper.mapToV2(resource);

        assertThat(resourceEntityV2).isNotNull();
        assertThat(resourceEntityV2.getName()).isEqualTo(resource.getName());
        assertThat(resourceEntityV2.getType()).isEqualTo(resource.getType());
        assertThat(resourceEntityV2.isEnabled()).isEqualTo(resource.getEnabled());

        // Configuration is tested deeper in ConfigurationSerializationMapperTest
        assertThat(resourceEntityV2.getConfiguration()).isNotNull().isNotEmpty();
    }

    @Test
    void shouldMapFromResourceEntityV4() {
        io.gravitee.definition.model.v4.resource.Resource resourceEntityV4 = ResourceFixtures.aResourceEntityV4();
        var resourceEntity = resourceMapper.map(resourceEntityV4);

        assertThat(resourceEntity).isNotNull();
        assertThat(resourceEntity.getName()).isEqualTo(resourceEntityV4.getName());
        assertThat(resourceEntity.getType()).isEqualTo(resourceEntityV4.getType());
        assertThat(resourceEntity.getEnabled()).isEqualTo(resourceEntityV4.isEnabled());

        // Configuration is tested deeper in ConfigurationSerializationMapperTest
        assertThat(resourceEntity.getConfiguration()).isNotNull().isInstanceOf(LinkedHashMap.class);
    }

    @Test
    void shouldMapFromResourceEntityV2() throws JsonProcessingException {
        io.gravitee.definition.model.plugins.resources.Resource resourceEntityV2 = ResourceFixtures.aResourceEntityV2();
        var resourceEntity = resourceMapper.map(resourceEntityV2);

        assertThat(resourceEntity).isNotNull();
        assertThat(resourceEntity.getName()).isEqualTo(resourceEntityV2.getName());
        assertThat(resourceEntity.getType()).isEqualTo(resourceEntityV2.getType());
        assertThat(resourceEntity.getEnabled()).isEqualTo(resourceEntityV2.isEnabled());

        // Configuration is tested deeper in ConfigurationSerializationMapperTest
        assertThat(resourceEntity.getConfiguration()).isNotNull().isInstanceOf(LinkedHashMap.class);
    }
}
