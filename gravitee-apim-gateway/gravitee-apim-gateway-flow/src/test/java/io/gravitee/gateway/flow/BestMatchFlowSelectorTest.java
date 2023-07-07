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
package io.gravitee.gateway.flow;

import static org.assertj.core.api.Assertions.*;

import io.gravitee.definition.model.flow.Flow;
import io.gravitee.definition.model.flow.PathOperator;
import org.junit.Before;
import org.junit.Test;

/**
 * @author Yann TAVERNIER (yann.tavernier at graviteesource.com)
 * @author GraviteeSource Team
 */
public class BestMatchFlowSelectorTest {

    private BestMatchFlowSelector cut;

    @Before
    public void setUp() {
        cut = new BestMatchFlowSelector();
    }

    @Test
    public void should_return_empty_when_flow_null() {
        assertThat(cut.providePath(null)).isEmpty();
    }

    @Test
    public void should_return_empty_string_when_flow_has_no_path() {
        assertThat(cut.providePath(new Flow())).isNotEmpty().containsSame("");
    }

    @Test
    public void should_return_flow_path() {
        final Flow flow = new Flow();
        final PathOperator pathOperator = new PathOperator();
        pathOperator.setPath("path");
        flow.setPathOperator(pathOperator);
        assertThat(cut.providePath(flow)).isNotEmpty().containsSame("path");
    }
}
