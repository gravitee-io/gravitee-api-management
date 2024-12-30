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

import com.fasterxml.jackson.core.JsonProcessingException;
import fixtures.EntrypointFixtures;
import io.gravitee.definition.jackson.datatype.GraviteeMapper;
import io.gravitee.definition.model.v4.listener.entrypoint.Qos;
import java.util.LinkedHashMap;
import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;

public class EntrypointMapperTest {

    private final EntrypointMapper entrypointMapper = Mappers.getMapper(EntrypointMapper.class);

    @Test
    void shouldMapToEntrypointEntity() throws JsonProcessingException {
        var entrypoint = EntrypointFixtures.anEntrypointHttpV4();

        var entrypointEntity = entrypointMapper.mapToHttpV4(entrypoint);
        assertThat(entrypointEntity).isNotNull();
        assertThat(entrypointEntity.getType()).isEqualTo(entrypoint.getType());
        assertThat(entrypointEntity.getConfiguration()).isEqualTo(new GraviteeMapper().writeValueAsString(entrypoint.getConfiguration()));
        assertThat(entrypointEntity.getDlq()).isNotNull();
        assertThat(entrypointEntity.getDlq().getEndpoint()).isEqualTo(entrypoint.getDlq().getEndpoint());
        assertThat(entrypointEntity.getQos()).isEqualTo(Qos.valueOf(entrypoint.getQos().name()));
    }

    @Test
    void shouldMapFromEntrypointEntity() throws JsonProcessingException {
        var entrypointEntity = EntrypointFixtures.aModelEntrypointHttpV4();

        var entrypoint = entrypointMapper.mapFromHttpV4(entrypointEntity);
        assertThat(entrypoint).isNotNull();
        assertThat(entrypoint.getType()).isEqualTo(entrypointEntity.getType());
        assertThat(entrypoint.getConfiguration())
            .isEqualTo(new GraviteeMapper().readValue(entrypointEntity.getConfiguration(), LinkedHashMap.class));
        assertThat(entrypoint.getDlq()).isNotNull();
        assertThat(entrypoint.getDlq().getEndpoint()).isEqualTo(entrypointEntity.getDlq().getEndpoint());
        assertThat(entrypoint.getQos())
            .isEqualTo(io.gravitee.rest.api.management.v2.rest.model.Qos.valueOf(entrypointEntity.getQos().name()));
    }

    @Test
    void shouldMapToNativeEntrypoint() throws JsonProcessingException {
        var entrypoint = EntrypointFixtures.anEntrypointNativeV4();

        var entrypointEntity = entrypointMapper.mapToNativeV4(entrypoint);
        assertThat(entrypointEntity).isNotNull();
        assertThat(entrypointEntity.getType()).isEqualTo(entrypoint.getType());
        assertThat(entrypointEntity.getConfiguration()).isEqualTo(new GraviteeMapper().writeValueAsString(entrypoint.getConfiguration()));
    }

    @Test
    void shouldMapFromNativeEntrypoint() throws JsonProcessingException {
        var entrypointEntity = EntrypointFixtures.aModelEntrypointNativeV4();

        var entrypoint = entrypointMapper.mapFromNativeV4(entrypointEntity);
        assertThat(entrypoint).isNotNull();
        assertThat(entrypoint.getType()).isEqualTo(entrypointEntity.getType());
        assertThat(entrypoint.getConfiguration())
            .isEqualTo(new GraviteeMapper().readValue(entrypointEntity.getConfiguration(), LinkedHashMap.class));
        assertThat(entrypoint.getDlq()).isNull();
        assertThat(entrypoint.getQos()).isNull();
    }
}
