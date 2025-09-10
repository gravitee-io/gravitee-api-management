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
package io.gravitee.apim.core.api.domain_service;

import io.gravitee.apim.core.api.model.Api;
import io.gravitee.apim.core.audit.model.AuditInfo;
import io.gravitee.common.event.EventManager;
import jakarta.validation.constraints.NotBlank;

/**
 * @author Yann TAVERNIER (yann.tavernier at graviteesource.com)
 * @author GraviteeSource Team
 */
public interface ApiStateDomainService {
    boolean isSynchronized(Api api, AuditInfo auditInfo);
    Api deploy(Api apiToDeploy, String deploymentLabel, AuditInfo auditInfo);
    Api start(Api api, AuditInfo auditInfo);
    boolean startV2DynamicProperties(String id);
    boolean startV4DynamicProperties(String apiId);
    Api stop(Api api, AuditInfo auditInfo);
    boolean stopV2DynamicProperties(String apiId);
    boolean stopV4DynamicProperties(@NotBlank String id);
}
