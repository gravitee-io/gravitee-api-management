package io.gravitee.definition.jackson.datatype.deser;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.deser.std.StdScalarDeserializer;
import io.gravitee.definition.model.HttpProxy;

import java.io.IOException;

/**
 * @author David BRASSELY (brasseld at gmail.com)
 * @author Gravitee.io Team
 */
public class HttpProxyDeserializer extends StdScalarDeserializer<HttpProxy> {

    public HttpProxyDeserializer(Class<HttpProxy> vc) {
        super(vc);
    }

    @Override
    public HttpProxy deserialize(JsonParser jp, DeserializationContext ctxt)
            throws IOException {
        JsonNode node = jp.getCodec().readTree(jp);

        HttpProxy httpProxy = new HttpProxy();

        httpProxy.setHost(readStringValue(node, "host"));

        String sPort = readStringValue(node, "port");
        if (sPort != null) {
            httpProxy.setPort(Integer.parseInt(sPort));
        }

        httpProxy.setPassword(readStringValue(node, "password"));
        httpProxy.setPrincipal(readStringValue(node, "principal"));

        return httpProxy;
    }

    private String readStringValue(JsonNode rootNode, String fieldName) {
        JsonNode fieldNode = rootNode.get(fieldName);
        if (fieldNode != null) {
            return fieldNode.asText();
        }

        return null;
    }
}