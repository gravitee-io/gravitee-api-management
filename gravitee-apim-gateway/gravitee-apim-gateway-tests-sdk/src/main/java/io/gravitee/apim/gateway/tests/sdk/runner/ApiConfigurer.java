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
package io.gravitee.apim.gateway.tests.sdk.runner;

import io.gravitee.definition.model.Api;
import io.gravitee.gateway.reactor.ReactableApi;

/**
 * @author Yann TAVERNIER (yann.tavernier at graviteesource.com)
 * @author GraviteeSource Team
 */
public interface ApiConfigurer {
    /**
     * Configure apis before their deployment.
     * @deprecated see {@link #configureApi(ReactableApi, Class)}, this one is to use for version of the Gateway prior to 3.19.0.
     * Useful to modify programmatically each api that will be deployed during the tests.
     * For example, add a JWT plan, or set endpoint groups.
     * @param api is the api to apply this function code
     */
    @Deprecated(since = "3.19.0")
    void configureApi(Api api);

    /**
     * Configure apis before their deployment.
     * Useful to modify programmatically each api that will be deployed during the tests.
     * For example, on api definition version under V4, add a JWT plan, or set endpoint groups.
     * @param reactableApi is the api to apply this function code
     * @param definitionClass is the class of the definition since V4 apis has a different model from previous versions.
     */
    void configureApi(ReactableApi<?> reactableApi, Class<?> definitionClass);
}
