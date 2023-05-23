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

import static io.gravitee.definition.model.DefinitionContext.*;
import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

import io.gravitee.rest.api.management.v2.rest.model.DefinitionContext;
import org.junit.jupiter.api.Test;

public class DefinitionContextMapperTest {

    private final DefinitionContextMapper definitionContextMapper = new DefinitionContextMapperImpl();

    @Test
    void shouldMapDefinitionContextWithManagementOriginString() {
        io.gravitee.definition.model.DefinitionContext definitionContext = new io.gravitee.definition.model.DefinitionContext(
            ORIGIN_MANAGEMENT,
            MODE_FULLY_MANAGED
        );

        io.gravitee.rest.api.management.v2.rest.model.DefinitionContext result = definitionContextMapper.map(definitionContext);

        assertThat(result).isNotNull();
        assertThat(result.getOrigin()).isEqualTo(io.gravitee.rest.api.management.v2.rest.model.DefinitionContext.OriginEnum.MANAGEMENT);
        assertThat(result.getMode()).isEqualTo(io.gravitee.rest.api.management.v2.rest.model.DefinitionContext.ModeEnum.FULLY_MANAGED);
    }

    @Test
    void shouldMapDefinitionContextWithKubernetesOriginString() {
        io.gravitee.definition.model.DefinitionContext definitionContext = new io.gravitee.definition.model.DefinitionContext(
            ORIGIN_KUBERNETES,
            MODE_API_DEFINITION_ONLY
        );

        io.gravitee.rest.api.management.v2.rest.model.DefinitionContext result = definitionContextMapper.map(definitionContext);

        assertThat(result).isNotNull();
        assertThat(result.getOrigin()).isEqualTo(DefinitionContext.OriginEnum.KUBERNETES);
        assertThat(result.getMode()).isEqualTo(DefinitionContext.ModeEnum.API_DEFINITION_ONLY);
    }

    @Test
    void shouldMapDefinitionContextWithManagementOriginEnum() {
        io.gravitee.rest.api.management.v2.rest.model.DefinitionContext definitionContext =
            new io.gravitee.rest.api.management.v2.rest.model.DefinitionContext();
        definitionContext.setOrigin(io.gravitee.rest.api.management.v2.rest.model.DefinitionContext.OriginEnum.MANAGEMENT);
        definitionContext.setMode(io.gravitee.rest.api.management.v2.rest.model.DefinitionContext.ModeEnum.FULLY_MANAGED);

        io.gravitee.definition.model.DefinitionContext result = definitionContextMapper.map(definitionContext);

        assertThat(result).isNotNull();
        assertThat(result.getOrigin()).isEqualTo(ORIGIN_MANAGEMENT);
        assertThat(result.getMode()).isEqualTo(MODE_FULLY_MANAGED);
    }

    @Test
    void shouldMapDefinitionContextWithKubernetesEnum() {
        io.gravitee.rest.api.management.v2.rest.model.DefinitionContext definitionContext =
            new io.gravitee.rest.api.management.v2.rest.model.DefinitionContext();
        definitionContext.setOrigin(io.gravitee.rest.api.management.v2.rest.model.DefinitionContext.OriginEnum.MANAGEMENT);
        definitionContext.setMode(io.gravitee.rest.api.management.v2.rest.model.DefinitionContext.ModeEnum.FULLY_MANAGED);

        io.gravitee.definition.model.DefinitionContext result = definitionContextMapper.map(definitionContext);

        assertThat(result).isNotNull();
        assertThat(result.getOrigin()).isEqualTo(ORIGIN_MANAGEMENT);
        assertThat(result.getMode()).isEqualTo(MODE_FULLY_MANAGED);
    }
}
