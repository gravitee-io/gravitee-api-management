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
package io.gravitee.apim.core.application.domain_service;

import io.gravitee.apim.core.audit.model.AuditInfo;
import io.gravitee.apim.core.validation.Validator;
import io.gravitee.rest.api.model.application.ApplicationSettings;

/**
 * @author Antoine CORDIER (antoine.cordier at graviteesource.com)
 * @author GraviteeSource Team
 */
public interface ValidateApplicationSettingsDomainService extends Validator<ValidateApplicationSettingsDomainService.Input> {
    record Input(AuditInfo auditInfo, String applicationId, ApplicationSettings settings) implements Validator.Input {
        public Input sanitized(ApplicationSettings sanitizedSettings) {
            return new Input(auditInfo, applicationId, sanitizedSettings);
        }
    }
}
