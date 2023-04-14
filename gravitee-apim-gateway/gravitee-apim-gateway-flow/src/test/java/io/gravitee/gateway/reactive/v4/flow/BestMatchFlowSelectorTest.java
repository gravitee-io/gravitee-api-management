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
package io.gravitee.gateway.reactive.v4.flow;

import static org.assertj.core.api.Assertions.assertThat;

import io.gravitee.definition.model.v4.flow.Flow;
import io.gravitee.definition.model.v4.flow.selector.HttpSelector;
import java.util.List;
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
        assertThat(cut.providePath(new Flow())).isEmpty();
    }

    @Test
    public void should_return_flow_path_when_http_selector() {
        final Flow flow = new Flow();
        final HttpSelector httpSelector = new HttpSelector();
        httpSelector.setPath("path");
        flow.setSelectors(List.of(httpSelector));
        assertThat(cut.providePath(flow)).isNotEmpty().containsSame("path");
    }
}
