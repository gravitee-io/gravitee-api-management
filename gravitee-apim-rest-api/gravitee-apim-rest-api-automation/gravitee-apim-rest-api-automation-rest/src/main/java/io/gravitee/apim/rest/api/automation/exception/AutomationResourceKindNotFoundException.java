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
package io.gravitee.apim.rest.api.automation.exception;

import io.gravitee.rest.api.service.exceptions.AbstractNotFoundException;
import java.util.Map;

/**
 * The module is loaded but serves nothing at the requested resource path.
 */
public class AutomationResourceKindNotFoundException extends AbstractNotFoundException {

    private final String module;
    private final String kindPath;

    public AutomationResourceKindNotFoundException(String module, String kindPath) {
        this.module = module;
        this.kindPath = kindPath;
    }

    @Override
    public String getMessage() {
        return "Automation module [" + module + "] serves no resource at [" + kindPath + "].";
    }

    @Override
    public String getTechnicalCode() {
        return "gamma.resource.kind.notFound";
    }

    @Override
    public Map<String, String> getParameters() {
        return Map.of("module", module, "kind", kindPath);
    }
}
