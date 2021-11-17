/*
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
import { PermissionsByScopes } from './permission';

export function fakePermissionsByScopes(attributes?: Partial<PermissionsByScopes>): PermissionsByScopes {
  const base: PermissionsByScopes = {
    ORGANIZATION: [
      'CUSTOM_USER_FIELDS',
      'ENTRYPOINT',
      'ENVIRONMENT',
      'IDENTITY_PROVIDER',
      'IDENTITY_PROVIDER_ACTIVATION',
      'INSTALLATION',
      'NOTIFICATION_TEMPLATES',
      'POLICIES',
      'ROLE',
      'SETTINGS',
      'TAG',
      'TENANT',
      'USER',
    ],
    ENVIRONMENT: [
      'ALERT',
      'API',
      'API_HEADER',
      'APPLICATION',
      'AUDIT',
      'CATEGORY',
      'CLIENT_REGISTRATION_PROVIDER',
      'DASHBOARD',
      'DICTIONARY',
      'DOCUMENTATION',
      'ENTRYPOINT',
      'GROUP',
      'IDENTITY_PROVIDER_ACTIVATION',
      'INSTANCE',
      'MESSAGE',
      'METADATA',
      'NOTIFICATION',
      'PLATFORM',
      'QUALITY_RULE',
      'SETTINGS',
      'TAG',
      'TENANT',
      'THEME',
      'TOP_APIS',
    ],
    API: [
      'ALERT',
      'ANALYTICS',
      'AUDIT',
      'DEFINITION',
      'DISCOVERY',
      'DOCUMENTATION',
      'EVENT',
      'GATEWAY_DEFINITION',
      'HEALTH',
      'LOG',
      'MEMBER',
      'MESSAGE',
      'METADATA',
      'NOTIFICATION',
      'PLAN',
      'QUALITY_RULE',
      'RATING',
      'RATING_ANSWER',
      'RESPONSE_TEMPLATES',
      'REVIEWS',
      'SUBSCRIPTION',
    ],
    APPLICATION: ['ALERT', 'ANALYTICS', 'DEFINITION', 'LOG', 'MEMBER', 'METADATA', 'NOTIFICATION', 'SUBSCRIPTION'],
  };

  return {
    ...base,
    ...attributes,
  };
}
