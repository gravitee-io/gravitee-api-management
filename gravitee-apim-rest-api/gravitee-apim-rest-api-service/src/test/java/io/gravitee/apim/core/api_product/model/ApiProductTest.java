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
package io.gravitee.apim.core.api_product.model;

import static org.assertj.core.api.Assertions.assertThat;

import io.gravitee.definition.jackson.datatype.GraviteeMapper;
import io.gravitee.definition.model.v4.flow.execution.FlowExecution;
import io.gravitee.definition.model.v4.flow.execution.FlowMode;
import org.junit.jupiter.api.Test;

class ApiProductTest {

    private final GraviteeMapper mapper = new GraviteeMapper(false);

    @Test
    void should_leave_the_flow_mode_out_when_written_as_json() throws Exception {
        var flowExecution = new FlowExecution();
        flowExecution.setMode(FlowMode.BEST_MATCH);
        var apiProduct = ApiProduct.builder().id("api-product-id").flowExecution(flowExecution).build();

        var json = mapper.writeValueAsString(apiProduct);

        assertThat(json).contains("api-product-id").doesNotContain("flowExecution");
    }

    @Test
    void should_read_the_flow_mode_from_json() throws Exception {
        var json = "{\"id\":\"api-product-id\",\"flowExecution\":{\"mode\":\"best-match\",\"matchRequired\":true}}";

        var apiProduct = mapper.readValue(json, ApiProduct.class);

        assertThat(apiProduct.getFlowExecution().getMode()).isEqualTo(FlowMode.BEST_MATCH);
        assertThat(apiProduct.getFlowExecution().isMatchRequired()).isTrue();
    }
}
