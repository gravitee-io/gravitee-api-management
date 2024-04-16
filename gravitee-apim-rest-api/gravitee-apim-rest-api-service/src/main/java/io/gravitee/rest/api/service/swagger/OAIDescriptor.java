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
package io.gravitee.rest.api.service.swagger;

import com.fasterxml.jackson.core.JsonProcessingException;
import io.swagger.v3.core.util.Json;
import io.swagger.v3.core.util.Json31;
import io.swagger.v3.core.util.Yaml;
import io.swagger.v3.core.util.Yaml31;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.SpecVersion;
import java.util.List;
import lombok.Getter;
import lombok.Setter;

/**
 * @author David BRASSELY (david.brassely at graviteesource.com)
 * @author GraviteeSource Team
 */
@Getter
public final class OAIDescriptor implements SwaggerDescriptor<OpenAPI> {

    private final OpenAPI oai;

    @Setter
    private List<String> messages;

    public OAIDescriptor(final OpenAPI oai) {
        this.oai = oai;
    }

    @Override
    public OpenAPI getSpecification() {
        return oai;
    }

    @Override
    public String toYaml() throws JsonProcessingException {
        if (SpecVersion.V31 == oai.getSpecVersion()) {
            return Yaml31.pretty(oai);
        } else {
            return Yaml.pretty(oai);
        }
    }

    @Override
    public String toJson() throws JsonProcessingException {
        if (SpecVersion.V31 == oai.getSpecVersion()) {
            return Json31.pretty(oai);
        } else {
            return Json.pretty(oai);
        }
    }
}
