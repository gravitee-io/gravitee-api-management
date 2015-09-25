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
package io.gravitee.gateway.core.definition.validator;

import io.gravitee.gateway.core.definition.ApiDefinition;
import io.gravitee.gateway.core.definition.HttpClientDefinition;
import io.gravitee.gateway.core.definition.ProxyDefinition;
import org.junit.Test;

import java.net.URI;

/**
 * @author David BRASSELY (brasseld at gmail.com)
 */
public class HttpClientValidatorTest {

    @Test(expected = ValidationException.class)
    public void validate_proxy_httpclient_useProxy() {
        HttpClientDefinition httpClientDefinition = new HttpClientDefinition();
        httpClientDefinition.setUseProxy(true);

        ProxyDefinition proxyDefinition = new ProxyDefinition();
        proxyDefinition.setHttpClient(httpClientDefinition);
        ApiDefinition definition = new ApiDefinition();
        definition.setProxy(proxyDefinition);

        new HttpClientValidator().validate(definition);
    }
}
