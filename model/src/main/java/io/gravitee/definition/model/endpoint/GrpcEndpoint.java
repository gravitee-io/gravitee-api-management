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
package io.gravitee.definition.model.endpoint;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.gravitee.definition.model.EndpointType;
import io.gravitee.definition.model.HttpClientOptions;
import io.gravitee.definition.model.ProtocolVersion;

/**
 * @author David BRASSELY (david.brassely at graviteesource.com)
 * @author GraviteeSource Team
 */
public class GrpcEndpoint extends HttpEndpoint {

    @JsonCreator
    public GrpcEndpoint(
        @JsonProperty("name") String name,
        @JsonProperty("target") String target,
        @JsonProperty("inherit") Boolean inherit
    ) {
        super(EndpointType.GRPC, name, target, inherit);
    }

    public GrpcEndpoint(String name, String target) {
        this(name, target, null);
    }

    @Override
    public HttpClientOptions getHttpClientOptionsJson() {
        final HttpClientOptions httpClientOptions = super.getHttpClientOptionsJson();
        if (httpClientOptions != null) {
            httpClientOptions.setVersion(ProtocolVersion.HTTP_2);
            return httpClientOptions;
        }
        return null;
    }

    @Override
    public void setHttpClientOptionsJson(HttpClientOptions httpClientOptions) {
        httpClientOptions.setVersion(ProtocolVersion.HTTP_2);
        super.setHttpClientOptionsJson(httpClientOptions);
    }
}
