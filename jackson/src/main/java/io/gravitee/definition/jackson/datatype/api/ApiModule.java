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
package io.gravitee.definition.jackson.datatype.api;

import io.gravitee.definition.jackson.datatype.GraviteeModule;
import io.gravitee.definition.jackson.datatype.api.deser.*;
import io.gravitee.definition.jackson.datatype.api.ser.*;
import io.gravitee.definition.model.*;

/**
 * @author David BRASSELY (david.brassely at graviteesource.com)
 * @author GraviteeSource Team
 */
public class ApiModule extends GraviteeModule {

    private static final long serialVersionUID = 1L;

    @SuppressWarnings("unchecked")
    public ApiModule() {
        super("api");

        // first deserializers
        addDeserializer(Api.class, new ApiDeserializer(Api.class));
        addDeserializer(Path.class, new PathDeserializer(Path.class));
        addDeserializer(Rule.class, new RuleDeserializer(Rule.class));
        addDeserializer(Policy.class, new PolicyDeserializer(Policy.class));
        addDeserializer(Proxy.class, new ProxyDeserializer(Proxy.class));
        addDeserializer(LoadBalancer.class, new LoadBalancerDeserializer(LoadBalancer.class));
        addDeserializer(Failover.class, new FailoverDeserializer(Failover.class));
        addDeserializer(HttpClient.class, new HttpClientDeserializer(HttpClient.class));
        addDeserializer(HttpProxy.class, new HttpProxyDeserializer(HttpProxy.class));
        addDeserializer(HttpClientOptions.class, new HttpClientOptionsDeserializer(HttpClientOptions.class));
        addDeserializer(HttpClientSslOptions.class, new HttpClientSslOptionsDeserializer(HttpClientSslOptions.class));
        addDeserializer(Endpoint.class, new EndpointDeserializer(Endpoint.class));

        // then serializers:
        addSerializer(Api.class, new ApiSerializer(Api.class));
        addSerializer(Path.class, new PathSerializer(Path.class));
        addSerializer(Rule.class, new RuleSerializer(Rule.class));
        addSerializer(Policy.class, new PolicySerializer(Policy.class));
        addSerializer(Proxy.class, new ProxySerializer(Proxy.class));
        addSerializer(LoadBalancer.class, new LoadBalancerSerializer(LoadBalancer.class));
        addSerializer(Failover.class, new FailoverSerializer(Failover.class));
        addSerializer(HttpClient.class, new HttpClientSerializer(HttpClient.class));
        addSerializer(HttpProxy.class, new HttpProxySerializer(HttpProxy.class));
        addSerializer(HttpClientOptions.class, new HttpClientOptionsSerializer(HttpClientOptions.class));
        addSerializer(HttpClientSslOptions.class, new HttpClientSslOptionsSerializer(HttpClientSslOptions.class));
        addSerializer(Endpoint.class, new EndpointSerializer(Endpoint.class));
    }
}
