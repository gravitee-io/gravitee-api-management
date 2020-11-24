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

import io.gravitee.rest.api.spec.converter.wsdl.WSDLToOpenAPIConverter;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.parser.core.models.AuthorizationValue;
import io.swagger.v3.parser.util.RemoteUrl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

/**
 * @author Eric LELEU (eric.leleu at graviteesource.com)
 * @author GraviteeSource Team
 */
public class WsdlParser extends AbstractSwaggerParser<OpenAPI> {
    private static final Logger LOGGER = LoggerFactory.getLogger(WsdlParser.class);

    @Override
    public OpenAPI parse(String content) {
        try {
            if (isLocationUrl(content)) {
                return new WSDLToOpenAPIConverter().toOpenAPI(RemoteUrl.urlToString(content, (List<AuthorizationValue>) null));
            } else {
                return new WSDLToOpenAPIConverter().toOpenAPI(content);
            }
        } catch (Exception e) {
            LOGGER.info("Wsdl parsing failed : {}", e.getMessage());
            return null;
        }
    }
}
