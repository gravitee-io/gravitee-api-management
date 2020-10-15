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
import io.swagger.parser.OpenAPIParser;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.parser.core.models.SwaggerParseResult;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
/**
 * @author David BRASSELY (david.brassely at graviteesource.com)
 * @author GraviteeSource Team
 */
public class OAIParser extends AbstractSwaggerParser<OpenAPI> {

    static {
        System.setProperty(String.format("%s.trustAll", io.swagger.v3.parser.util.RemoteUrl.class.getName()), Boolean.TRUE.toString());
    }

    @Override
    public OpenAPI parse(String content) {
        OpenAPIParser parser = new OpenAPIParser();
        SwaggerParseResult parseResult;
        String path = content;
        if (!isLocationUrl(content)) {
            // Swagger v1 supports only a URL to read swagger: create temporary file for Swagger parser
            File temp = createTempFile(content);
            path = temp.getAbsolutePath();
        }

        parseResult = parser.readLocation(path, null, null);

        if (parseResult != null && parseResult.getOpenAPI() != null &&
                (parseResult.getMessages() != null && !parseResult.getMessages().isEmpty())) {
            throw new SwaggerDescriptorException(parseResult.getMessages());
        }

        return (parseResult != null && parseResult.getOpenAPI() != null && parseResult.getOpenAPI().getInfo() != null)
                ? parseResult.getOpenAPI() : null;
    }

    private File createTempFile(String content) {
        File temp = null;
        String fileName = "gio_swagger_" + System.currentTimeMillis();
        BufferedWriter bw = null;
        FileWriter out = null;
        try {
            temp = File.createTempFile(fileName, ".tmp");
            out = new FileWriter(temp);
            bw = new BufferedWriter(out);
            bw.write(content);
            bw.close();
        } catch (IOException ioe) {
            // Fallback to the new parser
        } finally {
            if (bw != null) {
                try {
                    bw.close();
                } catch (IOException e) {
                }
            }
            if (out != null) {
                try {
                    out.close();
                } catch (IOException e) {
                }
            }
        }
        return temp;
    }

}
