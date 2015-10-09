package io.gravitee.definition.jackson.datatype.ser;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.ser.std.StdScalarSerializer;
import io.gravitee.definition.model.HttpClient;

import java.io.IOException;

/**
 * @author David BRASSELY (brasseld at gmail.com)
 * @author Gravitee.io Team
 */
public class HttpClientSerializer extends StdScalarSerializer<HttpClient> {

    public HttpClientSerializer(Class<HttpClient> t) {
        super(t);
    }

    @Override
    public void serialize(HttpClient httpClient, JsonGenerator jgen, SerializerProvider provider) throws IOException {
        jgen.writeStartObject();
        jgen.writeBooleanField("use_proxy", httpClient.isUseProxy());
        jgen.writeObjectField("http_proxy", httpClient.getHttpProxy());
        jgen.writeEndObject();
    }
}
