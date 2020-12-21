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
package io.gravitee.rest.api.model.settings;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import io.gravitee.rest.api.model.annotations.ParameterKey;
import io.gravitee.rest.api.model.parameters.Key;

/**
 * @author Florent CHAMFROY (florent.chamfroy at graviteesource.com)
 * @author GraviteeSource Team
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class OpenAPIDocViewer {
    private OpenAPIDocViewer.OpenAPIDocType openAPIDocType;

    public OpenAPIDocViewer() { openAPIDocType = new OpenAPIDocViewer.OpenAPIDocType(); }

    public OpenAPIDocViewer.OpenAPIDocType getOpenAPIDocType() { return openAPIDocType; }

    public void setOpenAPIDocType(OpenAPIDocViewer.OpenAPIDocType openAPIDocType) {
        this.openAPIDocType = openAPIDocType;
    }

    public static class OpenAPIDocType {
        @ParameterKey(Key.OPEN_API_DOC_TYPE_SWAGGER_ENABLED)
        private Enabled swagger;

        @ParameterKey(Key.OPEN_API_DOC_TYPE_REDOC_ENABLED)
        private Enabled redoc;

        @ParameterKey(Key.OPEN_API_DOC_TYPE_DEFAULT)
        private String defaultType;

        public Enabled getSwagger() {
            return swagger;
        }

        public void setSwagger(Enabled swagger) {
            this.swagger = swagger;
        }

        public Enabled getRedoc() {
            return redoc;
        }

        public void setRedoc(Enabled redoc) {
            this.redoc = redoc;
        }

        public String getDefaultType() {
            return defaultType;
        }

        public void setDefaultType(String defaultType) {
            this.defaultType = defaultType;
        }
    }
}