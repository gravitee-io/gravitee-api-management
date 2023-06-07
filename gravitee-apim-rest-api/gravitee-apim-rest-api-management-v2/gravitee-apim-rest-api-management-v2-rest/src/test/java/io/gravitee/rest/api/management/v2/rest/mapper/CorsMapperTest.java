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

import fixtures.CorsFixtures;
import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;

public class CorsMapperTest {

    private final CorsMapper corsMapper = Mappers.getMapper(CorsMapper.class);

    @Test
    void shouldMapToCorsEntity() {
        var cors = CorsFixtures.aCors();

        var corsEntity = corsMapper.map(cors);
        assertThat(corsEntity).isNotNull();
        assertThat(corsEntity.isEnabled()).isEqualTo(cors.getEnabled());
        assertThat(corsEntity.getAccessControlAllowOrigin()).isEqualTo(cors.getAllowOrigin());
        assertThat(corsEntity.getAccessControlAllowHeaders()).isEqualTo(cors.getAllowHeaders());
        assertThat(corsEntity.getAccessControlAllowMethods()).isEqualTo(cors.getAllowMethods());
        assertThat(corsEntity.isAccessControlAllowCredentials()).isEqualTo(cors.getAllowCredentials());
        assertThat(corsEntity.getAccessControlExposeHeaders()).isEqualTo(cors.getExposeHeaders());
        assertThat(corsEntity.getAccessControlMaxAge()).isEqualTo(cors.getMaxAge());
        assertThat(corsEntity.isRunPolicies()).isEqualTo(cors.getRunPolicies());
    }

    @Test
    void shouldMapFromCorsEntity() {
        var corsEntity = CorsFixtures.aModelCors();

        var cors = corsMapper.map(corsEntity);
        assertThat(cors).isNotNull();
        assertThat(cors.getEnabled()).isEqualTo(corsEntity.isEnabled());
        assertThat(cors.getAllowOrigin()).isEqualTo(corsEntity.getAccessControlAllowOrigin());
        assertThat(cors.getAllowHeaders()).isEqualTo(corsEntity.getAccessControlAllowHeaders());
        assertThat(cors.getAllowMethods()).isEqualTo(corsEntity.getAccessControlAllowMethods());
        assertThat(cors.getAllowCredentials()).isEqualTo(corsEntity.isAccessControlAllowCredentials());
        assertThat(cors.getExposeHeaders()).isEqualTo(corsEntity.getAccessControlExposeHeaders());
        assertThat(cors.getMaxAge()).isEqualTo(corsEntity.getAccessControlMaxAge());
        assertThat(cors.getRunPolicies()).isEqualTo(corsEntity.isRunPolicies());
    }
}
