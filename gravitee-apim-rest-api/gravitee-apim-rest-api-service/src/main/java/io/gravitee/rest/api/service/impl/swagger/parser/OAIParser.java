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
package io.gravitee.rest.api.service.impl.swagger.parser;

import io.gravitee.rest.api.service.exceptions.SwaggerDescriptorException;
import io.gravitee.rest.api.service.exceptions.TechnicalManagementException;
import io.gravitee.rest.api.service.swagger.OAIDescriptor;
import io.swagger.parser.OpenAPIParser;
import io.swagger.v3.parser.core.models.ParseOptions;
import io.swagger.v3.parser.core.models.SwaggerParseResult;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.List;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author David BRASSELY (david.brassely at graviteesource.com)
 * @author GraviteeSource Team
 */
public class OAIParser extends AbstractDescriptorParser<OAIDescriptor> {

    private final Logger logger = LoggerFactory.getLogger(OAIParser.class);

    static {
        System.setProperty(String.format("%s.trustAll", io.swagger.v3.parser.util.RemoteUrl.class.getName()), Boolean.TRUE.toString());
    }

    public OAIDescriptor parse(String content, ParseOptions options) {
        OpenAPIParser parser = new OpenAPIParser();
        SwaggerParseResult parseResult;
        String path = content;
        File temp = null;
        if (!isLocationUrl(content)) {
            // Swagger v1 supports only a URL to read swagger: create temporary file for Swagger parser
            temp = createTempFile(content);
            path = temp.getAbsolutePath();
        }

        parseResult = parser.readLocation(path, null, options);

        if (temp != null) {
            if (!temp.delete()) {
                // Do nothing if failed
            }
        }

        /* Hack due to swagger v1 converting issue
         * See https://github.com/swagger-api/swagger-parser/issues/1451
         */
        if (parseResult.getMessages() != null) {
            final List<String> filteredMessages = parseResult
                .getMessages()
                .stream()
                .filter(message -> message != null && !message.matches("^attribute info.contact.*"))
                .collect(Collectors.toList());
            parseResult.setMessages(filteredMessages);
        }

        if (parseResult.getOpenAPI() == null) {
            throw new SwaggerDescriptorException("Malformed descriptor");
        }

        OAIDescriptor descriptor = new OAIDescriptor(parseResult.getOpenAPI());
        descriptor.setMessages(parseResult.getMessages());
        return descriptor;
    }

    @Override
    public OAIDescriptor parse(String content) {
        return parse(content, null);
    }

    private File createTempFile(String content) {
        String fileName = "gio_swagger_" + System.currentTimeMillis();
        File temp;
        try {
            temp = File.createTempFile(fileName, ".tmp");
        } catch (IOException ioe) {
            throw new TechnicalManagementException("An error occurs while trying to create temp file", ioe);
        }
        try (FileWriter out = new FileWriter(temp); BufferedWriter bw = new BufferedWriter(out)) {
            bw.write(content);
        } catch (IOException ioe) {
            throw new TechnicalManagementException("An error occurs while trying to create temp file", ioe);
        }
        return temp;
    }
}
