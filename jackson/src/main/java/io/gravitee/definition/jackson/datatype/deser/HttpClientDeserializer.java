package io.gravitee.definition.jackson.datatype.deser;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.deser.std.StdScalarDeserializer;
import io.gravitee.definition.model.HttpClient;
import io.gravitee.definition.model.HttpProxy;

import java.io.IOException;

/**
 * @author David BRASSELY (brasseld at gmail.com)
 * @author Gravitee.io Team
 */
public class HttpClientDeserializer extends StdScalarDeserializer<HttpClient> {

    public HttpClientDeserializer(Class<HttpClient> vc) {
        super(vc);
    }

    @Override
    public HttpClient deserialize(JsonParser jp, DeserializationContext ctxt)
            throws IOException {
        JsonNode node = jp.getCodec().readTree(jp);

        HttpClient httpClient = new HttpClient();

        JsonNode useProxyNode = node.get("use_proxy");
        if (useProxyNode != null) {
            boolean useProxy = useProxyNode.asBoolean(false);
            httpClient.setUseProxy(useProxy);
        }

        JsonNode httpProxyNode = node.get("http_proxy");
        if (httpProxyNode != null) {
            HttpProxy httpProxy = httpProxyNode.traverse(jp.getCodec()).readValueAs(HttpProxy.class);
            httpClient.setHttpProxy(httpProxy);
        }

        return httpClient;
    }
}