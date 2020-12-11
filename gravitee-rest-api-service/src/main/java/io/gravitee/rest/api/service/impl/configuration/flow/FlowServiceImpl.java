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
package io.gravitee.rest.api.service.impl.configuration.flow;

import io.gravitee.rest.api.service.configuration.flow.FlowService;
import io.gravitee.rest.api.service.exceptions.TechnicalManagementException;
import org.apache.commons.io.IOUtils;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.InputStream;

import static java.nio.charset.Charset.defaultCharset;

/**
 * @author Guillaume CUSNIEUX (guillaume.cusnieux at graviteesource.com)
 * @author GraviteeSource Team
 */
@Component
public class FlowServiceImpl implements FlowService {

    private static final String DEFINITION_PATH = "/flow/apim-schema.json";

    @Override
    public String getSchema() {
        try {
            InputStream resourceAsStream = this.getClass().getResourceAsStream(DEFINITION_PATH);
            return IOUtils.toString(resourceAsStream, defaultCharset());
        } catch (IOException e) {
            throw new TechnicalManagementException("An error occurs while trying load flow definition", e);
        }
    }
}


