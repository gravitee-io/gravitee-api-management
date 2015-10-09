package io.gravitee.definition.jackson.datatype.ser;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.ser.std.StdScalarSerializer;
import io.gravitee.definition.model.HttpClient;
import io.gravitee.definition.model.HttpProxy;

import java.io.IOException;

/**
 * @author David BRASSELY (brasseld at gmail.com)
 * @author Gravitee.io Team
 */
public class HttpProxySerializer extends StdScalarSerializer<HttpProxy> {

    public HttpProxySerializer(Class<HttpProxy> t) {
        super(t);
    }

    @Override
    public void serialize(HttpProxy httpProxy, JsonGenerator jgen, SerializerProvider provider) throws IOException {
        jgen.writeStartObject();
        jgen.writeStringField("host", httpProxy.getHost());
        jgen.writeNumberField("port", httpProxy.getPort());
        jgen.writeStringField("principal", httpProxy.getPrincipal());
        jgen.writeStringField("password", httpProxy.getPassword());
        jgen.writeEndObject();
    }
}
