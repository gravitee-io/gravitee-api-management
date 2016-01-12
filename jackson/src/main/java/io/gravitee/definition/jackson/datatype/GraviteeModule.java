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
package io.gravitee.definition.jackson.datatype;

import com.fasterxml.jackson.core.Version;
import com.fasterxml.jackson.databind.module.SimpleModule;
import io.gravitee.definition.jackson.datatype.deser.*;
import io.gravitee.definition.jackson.datatype.ser.*;
import io.gravitee.definition.model.*;

/**
 * @author David BRASSELY (brasseld at gmail.com)
 * @author Gravitee.io Team
 */
public class GraviteeModule extends SimpleModule {
    private static final long serialVersionUID = 1L;

    @SuppressWarnings("unchecked")
    public GraviteeModule() {
        super(new Version(0, 1, 0, (String)null, (String)null, (String)null));

        // first deserializers
        addDeserializer(Api.class, new ApiDeserializer(Api.class));
        addDeserializer(Path.class, new PathDeserializer(Path.class));
        addDeserializer(Rule.class, new RuleDeserializer(Rule.class));
        addDeserializer(Policy.class, new PolicyDeserializer(Policy.class));
        addDeserializer(Proxy.class, new ProxyDeserializer(Proxy.class));
        addDeserializer(HttpClient.class, new HttpClientDeserializer(HttpClient.class));
        addDeserializer(HttpProxy.class, new HttpProxyDeserializer(HttpProxy.class));
        addDeserializer(HttpClientOptions.class, new HttpClientOptionsDeserializer(HttpClientOptions.class));
        addDeserializer(HttpClientSslOptions.class, new HttpClientSslOptionsDeserializer(HttpClientSslOptions.class));
        addDeserializer(Monitoring.class, new MonitoringDeserializer(Monitoring.class));

        // then serializers:
        addSerializer(Api.class, new ApiSerializer(Api.class));
        addSerializer(Path.class, new PathSerializer(Path.class));
        addSerializer(Rule.class, new RuleSerializer(Rule.class));
        addSerializer(Policy.class, new PolicySerializer(Policy.class));
        addSerializer(Proxy.class, new ProxySerializer(Proxy.class));
        addSerializer(HttpClient.class, new HttpClientSerializer(HttpClient.class));
        addSerializer(HttpProxy.class, new HttpProxySerializer(HttpProxy.class));
        addSerializer(HttpClientOptions.class, new HttpClientOptionsSerializer(HttpClientOptions.class));
        addSerializer(HttpClientSslOptions.class, new HttpClientSslOptionsSerializer(HttpClientSslOptions.class));
        addSerializer(Monitoring.class, new MonitoringSerializer(Monitoring.class));
    }

    @Override
    public String getModuleName() {
        // yes, will try to avoid duplicate registations (if MapperFeature enabled)
        return getClass().getSimpleName();
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }

    @Override
    public boolean equals(Object o) {
        return this == o;
    }
}
