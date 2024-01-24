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
package io.gravitee.apim.plugin.apiservice.dynamicproperties.http.helper;

import static io.gravitee.apim.plugin.apiservice.dynamicproperties.http.HttpDynamicPropertiesService.HTTP_DYNAMIC_PROPERTIES_TYPE;
import static org.assertj.core.api.Assertions.assertThat;

import io.gravitee.definition.model.v4.Api;
import io.gravitee.definition.model.v4.service.ApiServices;
import io.gravitee.definition.model.v4.service.Service;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Test;

/**
 * @author Yann TAVERNIER (yann.tavernier at graviteesource.com)
 * @author GraviteeSource Team
 */
@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
class HttpDynamicPropertiesHelperTest {

    @Test
    void should_not_handle_null_api() {
        assertThat(HttpDynamicPropertiesHelper.canHandle(null)).isFalse();
    }

    @Test
    void should_not_handle_null_api_services() {
        assertThat(HttpDynamicPropertiesHelper.canHandle(Api.builder().build())).isFalse();
    }

    @Test
    void should_not_handle_null_api_dynamic_properties_service() {
        assertThat(HttpDynamicPropertiesHelper.canHandle(Api.builder().services(new ApiServices()).build())).isFalse();
    }

    @Test
    void should_not_handle_disabled_dynamic_properties_service() {
        final ApiServices services = new ApiServices();
        services.setDynamicProperty(Service.builder().enabled(false).build());
        assertThat(HttpDynamicPropertiesHelper.canHandle(Api.builder().services(services).build())).isFalse();
    }

    @Test
    void should_not_handle_non_http_dynamic_properties_service() {
        final ApiServices services = new ApiServices();
        services.setDynamicProperty(Service.builder().enabled(true).type("not-" + HTTP_DYNAMIC_PROPERTIES_TYPE).build());
        assertThat(HttpDynamicPropertiesHelper.canHandle(Api.builder().services(services).build())).isFalse();
    }

    @Test
    void should_handle_http_dynamic_properties_service() {
        final ApiServices services = new ApiServices();
        services.setDynamicProperty(Service.builder().enabled(true).type(HTTP_DYNAMIC_PROPERTIES_TYPE).build());
        assertThat(HttpDynamicPropertiesHelper.canHandle(Api.builder().services(services).build())).isTrue();
    }
}
