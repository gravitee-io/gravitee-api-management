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

import java.util.EnumSet;
import org.junit.jupiter.api.Test;

class ApiProductKindFilterTest {

    @Test
    void classicOnly_excludes_every_specialized_kind() {
        assertThat(ApiProductKindFilter.classicOnly().excludedKinds()).isEqualTo(EnumSet.allOf(ApiProductKind.class));
    }

    @Test
    void any_excludes_no_kind() {
        assertThat(ApiProductKindFilter.any().excludedKinds()).isEmpty();
    }
}
