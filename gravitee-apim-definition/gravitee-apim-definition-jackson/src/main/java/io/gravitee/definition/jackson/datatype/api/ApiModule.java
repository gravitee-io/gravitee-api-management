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
package io.gravitee.definition.jackson.datatype.api;

import io.gravitee.definition.jackson.datatype.GraviteeMapper;
import io.gravitee.definition.jackson.datatype.GraviteeModule;
import io.gravitee.definition.jackson.datatype.api.deser.ApiDeserializer;
import io.gravitee.definition.jackson.datatype.api.deser.ConsumerDeserializer;
import io.gravitee.definition.jackson.datatype.api.deser.CorsDeserializer;
import io.gravitee.definition.jackson.datatype.api.deser.EndpointDeserializer;
import io.gravitee.definition.jackson.datatype.api.deser.EndpointGroupDeserializer;
import io.gravitee.definition.jackson.datatype.api.deser.FailoverDeserializer;
import io.gravitee.definition.jackson.datatype.api.deser.FlowV2Deserializer;
import io.gravitee.definition.jackson.datatype.api.deser.HttpClientOptionsDeserializer;
import io.gravitee.definition.jackson.datatype.api.deser.HttpClientSslOptionsDeserializer;
import io.gravitee.definition.jackson.datatype.api.deser.HttpProxyDeserializer;
import io.gravitee.definition.jackson.datatype.api.deser.LoadBalancerDeserializer;
import io.gravitee.definition.jackson.datatype.api.deser.LoggingDeserializer;
import io.gravitee.definition.jackson.datatype.api.deser.PolicyDeserializer;
import io.gravitee.definition.jackson.datatype.api.deser.PropertiesDeserializer;
import io.gravitee.definition.jackson.datatype.api.deser.PropertyDeserializer;
import io.gravitee.definition.jackson.datatype.api.deser.ProxyDeserializer;
import io.gravitee.definition.jackson.datatype.api.deser.ResponseTemplateDeserializer;
import io.gravitee.definition.jackson.datatype.api.deser.RuleDeserializer;
import io.gravitee.definition.jackson.datatype.api.deser.StepV2Deserializer;
import io.gravitee.definition.jackson.datatype.api.deser.VirtualHostDeserializer;
import io.gravitee.definition.jackson.datatype.api.deser.ssl.JKSKeyStoreDeserializer;
import io.gravitee.definition.jackson.datatype.api.deser.ssl.JKSTrustStoreDeserializer;
import io.gravitee.definition.jackson.datatype.api.deser.ssl.PEMKeyStoreDeserializer;
import io.gravitee.definition.jackson.datatype.api.deser.ssl.PEMTrustStoreDeserializer;
import io.gravitee.definition.jackson.datatype.api.deser.ssl.PKCS12KeyStoreDeserializer;
import io.gravitee.definition.jackson.datatype.api.deser.ssl.PKCS12TrustStoreDeserializer;
import io.gravitee.definition.jackson.datatype.api.ser.ApiSerializer;
import io.gravitee.definition.jackson.datatype.api.ser.ConsumerSerializer;
import io.gravitee.definition.jackson.datatype.api.ser.CorsSerializer;
import io.gravitee.definition.jackson.datatype.api.ser.EndpointGroupSerializer;
import io.gravitee.definition.jackson.datatype.api.ser.EndpointSerializer;
import io.gravitee.definition.jackson.datatype.api.ser.FailoverSerializer;
import io.gravitee.definition.jackson.datatype.api.ser.FlowV2Serializer;
import io.gravitee.definition.jackson.datatype.api.ser.HttpClientOptionsSerializer;
import io.gravitee.definition.jackson.datatype.api.ser.HttpClientSslOptionsSerializer;
import io.gravitee.definition.jackson.datatype.api.ser.HttpProxySerializer;
import io.gravitee.definition.jackson.datatype.api.ser.LoadBalancerSerializer;
import io.gravitee.definition.jackson.datatype.api.ser.LoggingSerializer;
import io.gravitee.definition.jackson.datatype.api.ser.PolicySerializer;
import io.gravitee.definition.jackson.datatype.api.ser.PropertiesSerializer;
import io.gravitee.definition.jackson.datatype.api.ser.PropertySerializer;
import io.gravitee.definition.jackson.datatype.api.ser.ProxySerializer;
import io.gravitee.definition.jackson.datatype.api.ser.ResponseTemplateSerializer;
import io.gravitee.definition.jackson.datatype.api.ser.RuleSerializer;
import io.gravitee.definition.jackson.datatype.api.ser.StepV2Serializer;
import io.gravitee.definition.jackson.datatype.api.ser.VirtualHostSerializer;
import io.gravitee.definition.jackson.datatype.api.ser.ssl.JKSKeyStoreSerializer;
import io.gravitee.definition.jackson.datatype.api.ser.ssl.JKSTrustStoreSerializer;
import io.gravitee.definition.jackson.datatype.api.ser.ssl.PEMKeyStoreSerializer;
import io.gravitee.definition.jackson.datatype.api.ser.ssl.PEMTrustStoreSerializer;
import io.gravitee.definition.jackson.datatype.api.ser.ssl.PKCS12KeyStoreSerializer;
import io.gravitee.definition.jackson.datatype.api.ser.ssl.PKCS12TrustStoreSerializer;
import io.gravitee.definition.model.Api;
import io.gravitee.definition.model.Cors;
import io.gravitee.definition.model.Endpoint;
import io.gravitee.definition.model.EndpointGroup;
import io.gravitee.definition.model.Failover;
import io.gravitee.definition.model.HttpClientOptions;
import io.gravitee.definition.model.HttpClientSslOptions;
import io.gravitee.definition.model.HttpProxy;
import io.gravitee.definition.model.LoadBalancer;
import io.gravitee.definition.model.Logging;
import io.gravitee.definition.model.Policy;
import io.gravitee.definition.model.Properties;
import io.gravitee.definition.model.Property;
import io.gravitee.definition.model.Proxy;
import io.gravitee.definition.model.ResponseTemplate;
import io.gravitee.definition.model.Rule;
import io.gravitee.definition.model.VirtualHost;
import io.gravitee.definition.model.flow.Consumer;
import io.gravitee.definition.model.flow.FlowV2;
import io.gravitee.definition.model.flow.FlowV2Impl;
import io.gravitee.definition.model.flow.StepV2;
import io.gravitee.definition.model.ssl.jks.JKSKeyStore;
import io.gravitee.definition.model.ssl.jks.JKSTrustStore;
import io.gravitee.definition.model.ssl.pem.PEMKeyStore;
import io.gravitee.definition.model.ssl.pem.PEMTrustStore;
import io.gravitee.definition.model.ssl.pkcs12.PKCS12KeyStore;
import io.gravitee.definition.model.ssl.pkcs12.PKCS12TrustStore;

/**
 * @author David BRASSELY (david.brassely at graviteesource.com)
 * @author GraviteeSource Team
 */
public class ApiModule extends GraviteeModule {

    private static final long serialVersionUID = 1L;

    @SuppressWarnings("unchecked")
    public ApiModule(GraviteeMapper graviteeMapper) {
        super("api");
        // first deserializers
        addDeserializer(Api.class, new ApiDeserializer(Api.class));
        addDeserializer(Rule.class, new RuleDeserializer(Rule.class));
        addDeserializer(Policy.class, new PolicyDeserializer(Policy.class));
        addDeserializer(Proxy.class, new ProxyDeserializer(Proxy.class));
        addDeserializer(LoadBalancer.class, new LoadBalancerDeserializer(LoadBalancer.class));
        addDeserializer(Failover.class, new FailoverDeserializer(Failover.class));
        addDeserializer(HttpProxy.class, new HttpProxyDeserializer(HttpProxy.class));
        addDeserializer(HttpClientOptions.class, new HttpClientOptionsDeserializer(HttpClientOptions.class));
        addDeserializer(HttpClientSslOptions.class, new HttpClientSslOptionsDeserializer(HttpClientSslOptions.class));
        addDeserializer(Endpoint.class, new EndpointDeserializer(Endpoint.class, graviteeMapper));
        addDeserializer(Properties.class, new PropertiesDeserializer(Properties.class));
        addDeserializer(Property.class, new PropertyDeserializer(Property.class));
        addDeserializer(Cors.class, new CorsDeserializer(Cors.class));
        addDeserializer(EndpointGroup.class, new EndpointGroupDeserializer(EndpointGroup.class));
        addDeserializer(Logging.class, new LoggingDeserializer(Logging.class));
        addDeserializer(JKSKeyStore.class, new JKSKeyStoreDeserializer(JKSKeyStore.class));
        addDeserializer(PEMKeyStore.class, new PEMKeyStoreDeserializer(PEMKeyStore.class));
        addDeserializer(PKCS12KeyStore.class, new PKCS12KeyStoreDeserializer(PKCS12KeyStore.class));
        addDeserializer(PEMTrustStore.class, new PEMTrustStoreDeserializer(PEMTrustStore.class));
        addDeserializer(JKSTrustStore.class, new JKSTrustStoreDeserializer(JKSTrustStore.class));
        addDeserializer(PKCS12TrustStore.class, new PKCS12TrustStoreDeserializer(PKCS12TrustStore.class));
        addDeserializer(ResponseTemplate.class, new ResponseTemplateDeserializer(ResponseTemplate.class));
        addDeserializer(VirtualHost.class, new VirtualHostDeserializer(VirtualHost.class));
        addDeserializer(FlowV2.class, new FlowV2Deserializer(FlowV2.class));
        addDeserializer(StepV2.class, new StepV2Deserializer(StepV2.class));
        addDeserializer(Consumer.class, new ConsumerDeserializer(Consumer.class));

        // then serializers:
        addSerializer(Api.class, new ApiSerializer(Api.class));
        addSerializer(Rule.class, new RuleSerializer(Rule.class));
        addSerializer(Policy.class, new PolicySerializer(Policy.class));
        addSerializer(Proxy.class, new ProxySerializer(Proxy.class));
        addSerializer(LoadBalancer.class, new LoadBalancerSerializer(LoadBalancer.class));
        addSerializer(Failover.class, new FailoverSerializer(Failover.class));
        addSerializer(HttpProxy.class, new HttpProxySerializer(HttpProxy.class));
        addSerializer(HttpClientOptions.class, new HttpClientOptionsSerializer(HttpClientOptions.class));
        addSerializer(HttpClientSslOptions.class, new HttpClientSslOptionsSerializer(HttpClientSslOptions.class));
        addSerializer(Endpoint.class, new EndpointSerializer(Endpoint.class));
        addSerializer(Properties.class, new PropertiesSerializer(Properties.class));
        addSerializer(Property.class, new PropertySerializer(Property.class));
        addSerializer(Cors.class, new CorsSerializer(Cors.class));
        addSerializer(EndpointGroup.class, new EndpointGroupSerializer(EndpointGroup.class, graviteeMapper));
        addSerializer(Logging.class, new LoggingSerializer(Logging.class));
        addSerializer(JKSKeyStore.class, new JKSKeyStoreSerializer(JKSKeyStore.class));
        addSerializer(PEMKeyStore.class, new PEMKeyStoreSerializer(PEMKeyStore.class));
        addSerializer(PKCS12KeyStore.class, new PKCS12KeyStoreSerializer(PKCS12KeyStore.class));
        addSerializer(JKSTrustStore.class, new JKSTrustStoreSerializer(JKSTrustStore.class));
        addSerializer(PEMTrustStore.class, new PEMTrustStoreSerializer(PEMTrustStore.class));
        addSerializer(PKCS12TrustStore.class, new PKCS12TrustStoreSerializer(PKCS12TrustStore.class));
        addSerializer(ResponseTemplate.class, new ResponseTemplateSerializer(ResponseTemplate.class));
        addSerializer(VirtualHost.class, new VirtualHostSerializer(VirtualHost.class));
        addSerializer(FlowV2Impl.class, new FlowV2Serializer(FlowV2Impl.class));
        addSerializer(StepV2.class, new StepV2Serializer(StepV2.class));
        addSerializer(Consumer.class, new ConsumerSerializer(Consumer.class));
    }
}
