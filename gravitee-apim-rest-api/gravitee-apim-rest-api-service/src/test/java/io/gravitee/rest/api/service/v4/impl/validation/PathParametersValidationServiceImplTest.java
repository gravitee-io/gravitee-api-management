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
package io.gravitee.rest.api.service.v4.impl.validation;

import static org.assertj.core.api.Assertions.assertThatNoException;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.gravitee.definition.model.v4.ApiType;
import io.gravitee.definition.model.v4.flow.Flow;
import io.gravitee.definition.model.v4.flow.selector.HttpSelector;
import io.gravitee.rest.api.service.v4.exception.PathParameterOverlapValidationException;
import io.gravitee.rest.api.service.v4.validation.PathParametersValidationService;
import java.util.List;
import java.util.stream.Stream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * @author Yann TAVERNIER (yann.tavernier at graviteesource.com)
 * @author GraviteeSource Team
 */
@Deprecated
class PathParametersValidationServiceImplTest {

    private PathParametersValidationService cut;

    @BeforeEach
    void setUp() {
        cut = new PathParametersValidationServiceImpl();
    }

    @Test
    void should_not_throw_on_valid_openapi_with_same_hierarchy_different_param_names() {
        // Simulate flows for the OpenAPI spec in the problem statement
        Flow flow1 = new Flow();
        flow1.setEnabled(true);
        HttpSelector selector1 = new HttpSelector();
        selector1.setPath("/factFiles/:activityId");
        flow1.setSelectors(List.of(selector1));

        Flow flow2 = new Flow();
        flow2.setEnabled(true);
        HttpSelector selector2 = new HttpSelector();
        selector2.setPath("/factFiles/:activityId/:id");
        flow2.setSelectors(List.of(selector2));

        Flow flow3 = new Flow();
        flow3.setEnabled(true);
        HttpSelector selector3 = new HttpSelector();
        selector3.setPath("/facts/:locationNodeId/:timeNodeId/:versionNodeId");
        flow3.setSelectors(List.of(selector3));

        Flow flow4 = new Flow();
        flow4.setEnabled(true);
        HttpSelector selector4 = new HttpSelector();
        selector4.setPath("/facts/:locationNodeId/:timeNodeId/:versionNodeId/:id");
        flow4.setSelectors(List.of(selector4));

        // Add all flows to a stream
        Stream<Flow> flows = Stream.of(flow1, flow2, flow3, flow4);

        // Should not throw
        assertThatNoException().isThrownBy(() -> cut.validate(ApiType.PROXY, flows, Stream.empty()));
    }

    @Test
    void should_not_throw_on_different_segment_counts() {
        Flow flow1 = new Flow();
        flow1.setEnabled(true);
        HttpSelector selector1 = new HttpSelector();
        selector1.setPath("/products/:productId/items/:itemId");
        flow1.setSelectors(List.of(selector1));

        Flow flow2 = new Flow();
        flow2.setEnabled(true);
        HttpSelector selector2 = new HttpSelector();
        selector2.setPath("/:productId");
        flow2.setSelectors(List.of(selector2));

        assertThatNoException().isThrownBy(() -> cut.validate(ApiType.PROXY, Stream.of(flow1, flow2), Stream.empty()));
    }

    @Test
    void should_not_throw_on_static_vs_param_segment() {
        Flow flow1 = new Flow();
        flow1.setEnabled(true);
        HttpSelector selector1 = new HttpSelector();
        selector1.setPath("/products/:productId/items/:itemId");
        flow1.setSelectors(List.of(selector1));

        Flow flow2 = new Flow();
        flow2.setEnabled(true);
        HttpSelector selector2 = new HttpSelector();
        selector2.setPath("/products/:productId/items/static");
        flow2.setSelectors(List.of(selector2));

        assertThatNoException().isThrownBy(() -> cut.validate(ApiType.PROXY, Stream.of(flow1, flow2), Stream.empty()));
    }

    @Test
    void should_throw_on_same_structure_all_params() {
        Flow flow1 = new Flow();
        flow1.setEnabled(true);
        HttpSelector selector1 = new HttpSelector();
        selector1.setPath("/products/:productId/items/:itemId");
        flow1.setSelectors(List.of(selector1));

        Flow flow2 = new Flow();
        flow2.setEnabled(true);
        HttpSelector selector2 = new HttpSelector();
        selector2.setPath("/products/:id/items/:itemId");
        flow2.setSelectors(List.of(selector2));

        assertThatThrownBy(() -> cut.validate(ApiType.PROXY, Stream.of(flow1, flow2), Stream.empty())).isInstanceOf(
            PathParameterOverlapValidationException.class
        );
    }

    @Test
    void should_not_throw_on_duplicate_paths() {
        Flow flow1 = new Flow();
        flow1.setEnabled(true);
        HttpSelector selector1 = new HttpSelector();
        selector1.setPath("/products/:productId/items/:itemId");
        flow1.setSelectors(List.of(selector1));

        Flow flow2 = new Flow();
        flow2.setEnabled(true);
        HttpSelector selector2 = new HttpSelector();
        selector2.setPath("/products/:productId/items/:itemId");
        flow2.setSelectors(List.of(selector2));

        assertThatNoException().isThrownBy(() -> cut.validate(ApiType.PROXY, Stream.of(flow1, flow2), Stream.empty()));
    }
}
