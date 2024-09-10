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
package io.gravitee.apim.core.documentation.domain_service;

import io.gravitee.apim.core.documentation.model.PageSource;
import io.gravitee.apim.core.validation.Validator;

/**
 * @author Antoine CORDIER (antoine.cordier at graviteesource.com)
 * @author GraviteeSource Team
 */
public interface ValidatePageSourceDomainService extends Validator<ValidatePageSourceDomainService.Input> {
    record Input(String pageName, PageSource source) implements Validator.Input {
        public Input sanitized(PageSource source) {
            return new Input(pageName, source);
        }
    }
}
