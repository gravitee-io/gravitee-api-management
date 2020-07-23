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
package io.gravitee.management.service.impl.swagger.parser;

import io.gravitee.management.service.exceptions.SwaggerDescriptorException;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.parser.OpenAPIV3Parser;
import io.swagger.v3.parser.core.models.AuthorizationValue;
import io.swagger.v3.parser.core.models.SwaggerParseResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;
import java.util.List;

/**
 * @author David BRASSELY (david.brassely at graviteesource.com)
 * @author GraviteeSource Team
 */
public class OAIParser extends AbstractSwaggerParser<OpenAPI> {

    private final Logger logger = LoggerFactory.getLogger(OAIParser.class);

    static {
        System.setProperty(String.format("%s.trustAll", io.swagger.v3.parser.util.RemoteUrl.class.getName()), Boolean.TRUE.toString());
    }

    @Override
    public OpenAPI parse(String content) {
        OpenAPIV3Parser parser = new OpenAPIV3Parser();
        SwaggerParseResult parseResult;

        if (!isLocationUrl(content)) {
            parseResult = parser.readContents(content);
        } else {
            parseResult = parser.readWithInfo(content, (List<AuthorizationValue>) null);
        }

        if (parseResult != null && parseResult.getOpenAPI() != null &&
                (parseResult.getMessages() != null && !parseResult.getMessages().isEmpty())) {
            logger.error("Error while parsing OpenAPI descriptor: {}", parseResult.getMessages().get(0));
            throw new SwaggerDescriptorException();
        }

        return (parseResult != null && parseResult.getOpenAPI() != null && parseResult.getOpenAPI().getInfo() != null)
                ? parseResult.getOpenAPI() : null;
    }
}
