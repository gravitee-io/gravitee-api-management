package io.gravitee.apim.infra.specgen;

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
import static java.util.Optional.ofNullable;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.gravitee.apim.core.specgen.model.ApiSpecGen;
import io.gravitee.apim.core.specgen.service_provider.OasProvider;
import io.swagger.v3.core.util.Yaml;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.parser.OpenAPIV3Parser;
import io.swagger.v3.parser.core.models.ParseOptions;
import io.swagger.v3.parser.core.models.SwaggerParseResult;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * @author Rémi SULTAN (remi.sultan at graviteesource.com)
 * @author GraviteeSource Team
 */
@Service
@Slf4j
public class OasProviderImpl implements OasProvider {

    private static final ObjectMapper mapper = Yaml.mapper();

    @Override
    public String decorateSpecification(ApiSpecGen api, String content) {
        try {
            var parseResult = parse(content);
            var oas = parseResult.getOpenAPI();
            if (oas == null) {
                var messages = String.join(", ", parseResult.getMessages());
                log.warn("Could not parse generated OpenAPI Specification for api [{}], reason: [{}]", api.id(), messages);
                return content;
            }

            if (oas.getInfo() == null) {
                oas.setInfo(new Info());
            }
            var info = oas.getInfo();
            info.setTitle(api.name());
            info.setVersion(api.version());
            info.setDescription(getDescription(api, info.getDescription()));

            return mapper.writeValueAsString(oas);
        } catch (JsonProcessingException e) {
            log.warn("Could not write generated OpenAPI Specification for api [{}]", api.id(), e);
            return content;
        }
    }

    private static SwaggerParseResult parse(String content) {
        var options = new ParseOptions();
        options.setResolve(true);
        options.setResolveFully(true);

        return new OpenAPIV3Parser().readContents(content, null, options);
    }

    private static String getDescription(ApiSpecGen api, String oasDescription) {
        return ofNullable(api.description()).orElse(oasDescription);
    }
}
