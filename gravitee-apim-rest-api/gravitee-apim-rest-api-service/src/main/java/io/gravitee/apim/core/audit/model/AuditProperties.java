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
package io.gravitee.apim.core.audit.model;

public enum AuditProperties {
    PLAN,
    PAGE,
    API_KEY,
    METADATA,
    GROUP,
    USER,
    ROLE,
    API,
    APPLICATION,
    TAG,
    TENANT,
    CATEGORY,
    PARAMETER,
    DICTIONARY,
    API_HEADER,
    IDENTITY_PROVIDER,
    ENTRYPOINT,
    REQUEST_ID,
    CLIENT_REGISTRATION_PROVIDER,
    QUALITY_RULE,
    API_QUALITY_RULE,
    DASHBOARD,
    THEME,
    TOKEN,
    USER_FIELD,
    NOTIFICATION_TEMPLATE,
    SHARED_POLICY_GROUP,
}
