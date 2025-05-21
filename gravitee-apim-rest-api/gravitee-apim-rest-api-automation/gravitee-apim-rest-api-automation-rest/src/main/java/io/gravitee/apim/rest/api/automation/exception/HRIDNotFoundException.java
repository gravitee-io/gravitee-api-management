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
 * @author Antoine CORDIER (antoine.cordier at graviteesource.com)
 * @author GraviteeSource Team
 */
public class HRIDNotFoundException extends AbstractNotFoundException {

    private final String hrid;

    public HRIDNotFoundException(String hrid) {
        this.hrid = hrid;
    }

    @Override
    public String getMessage() {
        return "HRID [" + hrid + "] cannot be found.";
    }

    @Override
    public String getTechnicalCode() {
        return "hrid.notFound";
    }

    @Override
    public Map<String, String> getParameters() {
        return singletonMap("hrid", hrid);
    }
}
