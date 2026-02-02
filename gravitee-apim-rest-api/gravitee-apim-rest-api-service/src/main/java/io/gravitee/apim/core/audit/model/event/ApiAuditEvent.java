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
package io.gravitee.apim.core.audit.model.event;

/**
 * @author Yann TAVERNIER (yann.tavernier at graviteesource.com)
 * @author GraviteeSource Team
 */
public enum ApiAuditEvent implements AuditEvent {
    API_CREATED,
    API_UPDATED,
    API_DELETED,
    API_ROLLBACKED,
    API_LOGGING_ENABLED,
    API_LOGGING_DISABLED,
    API_LOGGING_UPDATED,
    METADATA_DELETED,
    METADATA_CREATED,
    METADATA_UPDATED,
    PUBLISH_API,
    PROMOTION_CREATED,
    AUTOMATION_DETACHED,
}
