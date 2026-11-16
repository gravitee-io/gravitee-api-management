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
package io.gravitee.apim.infra.converter.oai;

import static org.assertj.core.api.Assertions.assertThat;

import io.gravitee.definition.model.v4.flow.Flow;
import io.gravitee.definition.model.v4.flow.selector.HttpSelector;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.Operation;
import io.swagger.v3.oas.models.PathItem;
import io.swagger.v3.oas.models.Paths;
import java.util.List;
import org.junit.jupiter.api.Test;

class OAIToFlowsConverterTest {

    @Test
    void should_skip_invalid_wildcard_path_and_keep_the_other_paths() {
        var spec = specWithOperations("/pets", "/a/**", "/charges/{id}", "/users/@me", "/files/*");

        var flows = OAIToFlowsConverter.INSTANCE.convert(spec, List.of());

        assertThat(flows).extracting(OAIToFlowsConverterTest::httpPath).containsExactly("/pets", "/charges/:id", "/users/@me", "/files/*");
    }

    @Test
    void should_skip_every_operation_on_an_invalid_wildcard_path() {
        var wildcard = new PathItem();
        wildcard.setGet(new Operation());
        wildcard.setPost(new Operation());
        var spec = new OpenAPI();
        spec.setPaths(new Paths());
        spec.getPaths().addPathItem("/a/**", wildcard);

        var flows = OAIToFlowsConverter.INSTANCE.convert(spec, List.of());

        assertThat(flows).isEmpty();
    }

    private static OpenAPI specWithOperations(String... paths) {
        var spec = new OpenAPI();
        var pathMap = new Paths();
        for (var path : paths) {
            var pathItem = new PathItem();
            pathItem.setGet(new Operation());
            pathMap.addPathItem(path, pathItem);
        }
        spec.setPaths(pathMap);
        return spec;
    }

    private static String httpPath(Flow flow) {
        return flow
            .getSelectors()
            .stream()
            .filter(HttpSelector.class::isInstance)
            .map(HttpSelector.class::cast)
            .map(HttpSelector::getPath)
            .findFirst()
            .orElseThrow();
    }
}
