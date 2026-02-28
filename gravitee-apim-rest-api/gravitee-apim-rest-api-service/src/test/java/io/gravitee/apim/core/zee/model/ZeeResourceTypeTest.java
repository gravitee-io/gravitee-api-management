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
package io.gravitee.apim.core.zee.model;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
class ZeeResourceTypeTest {

    @ParameterizedTest
    @CsvSource({
            "FLOW,Flow",
            "PLAN,Plan",
            "API,Api",
            "ENDPOINT,Endpoint",
            "ENTRYPOINT,Entrypoint",
            "STEP,Step",
            "ENDPOINT_GROUP,EndpointGroup",
            "HTTP_LISTENER,HttpListener",
            "SUBSCRIPTION_LISTENER,SubscriptionListener",
    })
    void component_name_maps_correctly(ZeeResourceType type, String expectedName) {
        assertThat(type.componentName()).isEqualTo(expectedName);
    }

    @Test
    void all_nine_values_exist() {
        assertThat(ZeeResourceType.values()).hasSize(9);
    }
}
