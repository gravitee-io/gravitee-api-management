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
package io.gravitee.apim.plugin.apiservice.dynamicproperties.http;

import static io.gravitee.apim.plugin.apiservice.dynamicproperties.http.HttpDynamicPropertiesService.HTTP_DYNAMIC_PROPERTIES_TYPE;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

import io.gravitee.apim.rest.api.common.apiservices.DefaultManagementDeploymentContext;
import io.gravitee.definition.model.v4.Api;
import io.gravitee.definition.model.v4.service.ApiServices;
import io.gravitee.definition.model.v4.service.Service;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Test;
import org.springframework.context.ApplicationContext;

/**
 * @author Yann TAVERNIER (yann.tavernier at graviteesource.com)
 * @author GraviteeSource Team
 */
@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
class HttpDynamicPropertiesServiceFactoryTest {

    HttpDynamicPropertiesServiceFactory cut = new HttpDynamicPropertiesServiceFactory();

    @Test
    void should_return_null_when_service_cannot_be_handled() {
        assertThat(
            cut.createService(new DefaultManagementDeploymentContext(Api.builder().build(), mock(ApplicationContext.class)))
        ).isNull();
    }

    @Test
    void should_return_service_when_service_can_be_handled() {
        final ApiServices services = new ApiServices();
        services.setDynamicProperty(Service.builder().enabled(true).type(HTTP_DYNAMIC_PROPERTIES_TYPE).build());
        assertThat(
            cut.createService(
                new DefaultManagementDeploymentContext(Api.builder().services(services).build(), mock(ApplicationContext.class))
            )
        ).isInstanceOf(HttpDynamicPropertiesService.class);
    }
}
