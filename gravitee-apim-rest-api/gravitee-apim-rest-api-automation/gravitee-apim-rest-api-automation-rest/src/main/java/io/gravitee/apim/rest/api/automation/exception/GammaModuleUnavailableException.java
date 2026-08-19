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

import static java.util.Collections.singletonMap;

import io.gravitee.rest.api.service.exceptions.AbstractNotFoundException;
import java.util.Map;

/**
 * No loaded Gamma module answers on the requested module segment. From the automation surface's point of
 * view "not installed", "gamma disabled" and "not deployed because unlicensed" are indistinguishable, so the
 * message names all three.
 */
public class GammaModuleUnavailableException extends AbstractNotFoundException {

    private final String module;

    public GammaModuleUnavailableException(String module) {
        this.module = module;
    }

    @Override
    public String getMessage() {
        return (
            "No automation module [" +
            module +
            "] is available: it is not installed, gamma is disabled (gamma.enabled=false) or the license does not include it."
        );
    }

    @Override
    public String getTechnicalCode() {
        return "gamma.module.unavailable";
    }

    @Override
    public Map<String, String> getParameters() {
        return singletonMap("module", module);
    }
}
