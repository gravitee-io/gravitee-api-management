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
package io.gravitee.apim.infra.domain_service.api;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.gravitee.apim.core.api.domain_service.ApiDefinitionParserDomainService;
import io.gravitee.apim.core.api.exception.InvalidApiDefinitionException;
import io.gravitee.definition.model.v4.Api;
import java.io.IOException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class ApiDefinitionParserDomainServiceImpl implements ApiDefinitionParserDomainService {

    private final ObjectMapper objectMapper;

    public ApiDefinitionParserDomainServiceImpl(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Override
    public Api readV4ApiDefinition(String apiDefinition) {
        try {
            return objectMapper.readValue(apiDefinition, Api.class);
        } catch (IOException ioe) {
            throw new InvalidApiDefinitionException(ioe.getMessage());
        }
    }

    @Override
    public io.gravitee.definition.model.Api readV2ApiDefinition(String apiDefinition) {
        try {
            return objectMapper.readValue(apiDefinition, io.gravitee.definition.model.Api.class);
        } catch (IOException ioe) {
            throw new InvalidApiDefinitionException(ioe.getMessage());
        }
    }
}
