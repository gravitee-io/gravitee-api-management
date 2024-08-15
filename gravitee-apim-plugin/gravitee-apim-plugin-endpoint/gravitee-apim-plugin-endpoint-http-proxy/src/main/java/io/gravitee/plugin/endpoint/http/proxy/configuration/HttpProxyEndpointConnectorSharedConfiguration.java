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
package io.gravitee.plugin.endpoint.http.proxy.configuration;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.gravitee.common.http.HttpHeader;
import io.gravitee.definition.model.v4.http.HttpProxyOptions;
import io.gravitee.definition.model.v4.ssl.SslOptions;
import io.gravitee.gateway.reactive.api.connector.endpoint.EndpointConnectorSharedConfiguration;
import java.util.List;
import lombok.Getter;
import lombok.Setter;

/**
 * @author Yann TAVERNIER (yann.tavernier at graviteesource.com)
 * @author GraviteeSource Team
 */
@Getter
@Setter
public class HttpProxyEndpointConnectorSharedConfiguration implements EndpointConnectorSharedConfiguration {

    @JsonProperty("proxy")
    private HttpProxyOptions proxyOptions;

    @JsonProperty("http")
    private HttpClientOptions httpOptions = new HttpClientOptions();

    @JsonProperty("ssl")
    private SslOptions sslOptions;

    @JsonProperty("headers")
    private List<HttpHeader> headers;
}
