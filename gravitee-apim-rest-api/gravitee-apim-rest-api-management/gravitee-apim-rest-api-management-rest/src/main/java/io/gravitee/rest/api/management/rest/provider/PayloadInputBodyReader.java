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
package io.gravitee.rest.api.management.rest.provider;

import io.gravitee.rest.api.management.rest.model.PayloadInput;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.MultivaluedMap;
import jakarta.ws.rs.ext.MessageBodyReader;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import java.nio.charset.Charset;
import java.util.List;
import java.util.Optional;
import org.apache.http.NameValuePair;
import org.apache.http.client.utils.URLEncodedUtils;

/**
 * @author GraviteeSource Team
 */
public class PayloadInputBodyReader implements MessageBodyReader<PayloadInput> {

    @Override
    public boolean isReadable(Class<?> aClass, Type type, Annotation[] annotations, MediaType mediaType) {
        return (type == PayloadInput.class);
    }

    @Override
    public PayloadInput readFrom(
        Class<PayloadInput> aClass,
        Type type,
        Annotation[] annotations,
        MediaType mediaType,
        MultivaluedMap<String, String> multivaluedMap,
        InputStream inputStream
    ) throws IOException, WebApplicationException {
        try (BufferedReader br = new BufferedReader(new InputStreamReader(inputStream))) {
            final String line = br.readLine();
            final PayloadInput payloadInput = new PayloadInput();
            if (line != null) {
                final List<NameValuePair> params = URLEncodedUtils.parse(line, Charset.defaultCharset());
                payloadInput.setGrant_type(getParam(params, "grant_type"));
                payloadInput.setCode(getParam(params, "code"));
                payloadInput.setRedirect_uri(getParam(params, "redirect_uri"));
                payloadInput.setCode_verifier(getParam(params, "code_verifier"));
                payloadInput.setClient_id(getParam(params, "client_id"));
            }
            return payloadInput;
        }
    }

    private String getParam(final List<NameValuePair> params, final String paramName) {
        final Optional<NameValuePair> optionalParam = params
            .stream()
            .filter(param -> paramName.equals(param.getName()))
            .findAny();
        return optionalParam.map(NameValuePair::getValue).orElse(null);
    }
}
