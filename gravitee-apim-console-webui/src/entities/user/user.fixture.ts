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
import { User } from './user';

export const fakeAdminUser = (): User => {
  return {
    id: 'f7b34e11-a476-4af1-b34e-11a4768af103',
    roles: [
      {
        id: '90d39584-e4dc-482e-9395-84e4dcd82ea5',
        name: 'ADMIN',
        scope: 'ENVIRONMENT',
        permissions: {
          AUDIT: ['C', 'R', 'U', 'D'],
          QUALITY_RULE: ['C', 'R', 'U', 'D'],
          DOCUMENTATION: ['C', 'R', 'U', 'D'],
          API_HEADER: ['C', 'R', 'U', 'D'],
          APPLICATION: ['C', 'R', 'U', 'D'],
          THEME: ['C', 'R', 'U', 'D'],
          INSTANCE: ['C', 'R', 'U', 'D'],
          ALERT: ['C', 'R', 'U', 'D'],
          SETTINGS: ['C', 'R', 'U', 'D'],
          ENTRYPOINT: ['C', 'R', 'U', 'D'],
          TENANT: ['C', 'R', 'U', 'D'],
          TOP_APIS: ['C', 'R', 'U', 'D'],
          CLIENT_REGISTRATION_PROVIDER: ['C', 'R', 'U', 'D'],
          PLATFORM: ['C', 'R', 'U', 'D'],
          GROUP: ['C', 'R', 'U', 'D'],
          MESSAGE: ['C', 'R', 'U', 'D'],
          NOTIFICATION: ['C', 'R', 'U', 'D'],
          DASHBOARD: ['C', 'R', 'U', 'D'],
          CATEGORY: ['C', 'R', 'U', 'D'],
          DICTIONARY: ['C', 'R', 'U', 'D'],
          API: ['C', 'R', 'U', 'D'],
          TAG: ['C', 'R', 'U', 'D'],
          METADATA: ['C', 'R', 'U', 'D'],
          IDENTITY_PROVIDER_ACTIVATION: ['C', 'R', 'U', 'D'],
        },
      },
      {
        id: 'f9de9e21-d7d3-4662-9e9e-21d7d34662e6',
        name: 'ADMIN',
        scope: 'ORGANIZATION',
        permissions: {
          CUSTOM_USER_FIELDS: ['C', 'R', 'U', 'D'],
          ENVIRONMENT: ['C', 'R', 'U', 'D'],
          SETTINGS: ['C', 'R', 'U', 'D'],
          USER: ['C', 'R', 'U', 'D'],
          ENTRYPOINT: ['C', 'R', 'U', 'D'],
          TENANT: ['C', 'R', 'U', 'D'],
          POLICIES: ['C', 'R', 'U', 'D'],
          ROLE: ['C', 'R', 'U', 'D'],
          NOTIFICATION_TEMPLATES: ['C', 'R', 'U', 'D'],
          INSTALLATION: ['C', 'R', 'U', 'D'],
          TAG: ['C', 'R', 'U', 'D'],
          IDENTITY_PROVIDER: ['C', 'R', 'U', 'D'],
          IDENTITY_PROVIDER_ACTIVATION: ['C', 'R', 'U', 'D'],
        },
      },
    ],
    envRoles: {
      DEFAULT: [
        {
          id: '90d39584-e4dc-482e-9395-84e4dcd82ea5',
          name: 'ADMIN',
          scope: 'ENVIRONMENT',
          permissions: {
            AUDIT: ['C', 'R', 'U', 'D'],
            QUALITY_RULE: ['C', 'R', 'U', 'D'],
            DOCUMENTATION: ['C', 'R', 'U', 'D'],
            API_HEADER: ['C', 'R', 'U', 'D'],
            APPLICATION: ['C', 'R', 'U', 'D'],
            THEME: ['C', 'R', 'U', 'D'],
            INSTANCE: ['C', 'R', 'U', 'D'],
            ALERT: ['C', 'R', 'U', 'D'],
            SETTINGS: ['C', 'R', 'U', 'D'],
            ENTRYPOINT: ['C', 'R', 'U', 'D'],
            TENANT: ['C', 'R', 'U', 'D'],
            TOP_APIS: ['C', 'R', 'U', 'D'],
            CLIENT_REGISTRATION_PROVIDER: ['C', 'R', 'U', 'D'],
            PLATFORM: ['C', 'R', 'U', 'D'],
            GROUP: ['C', 'R', 'U', 'D'],
            MESSAGE: ['C', 'R', 'U', 'D'],
            NOTIFICATION: ['C', 'R', 'U', 'D'],
            DASHBOARD: ['C', 'R', 'U', 'D'],
            CATEGORY: ['C', 'R', 'U', 'D'],
            DICTIONARY: ['C', 'R', 'U', 'D'],
            API: ['C', 'R', 'U', 'D'],
            TAG: ['C', 'R', 'U', 'D'],
            METADATA: ['C', 'R', 'U', 'D'],
            IDENTITY_PROVIDER_ACTIVATION: ['C', 'R', 'U', 'D'],
          },
        },
      ],
    },
    source: 'memory',
    sourceId: 'admin',
    displayName: 'admin',
    lastConnectionAt: 1631017105654,
    firstConnectionAt: 1630373735403,
    status: 'ACTIVE',
    loginCount: 1094,
    newsletterSubscribed: false,
    created_at: 1630373733551,
    updated_at: 1631017105654,
    primary_owner: true,
    number_of_active_tokens: 1,
  };
};

export function fakeUser(attributes?: Partial<User>): User {
  const base: User = {
    id: 'f7b34e11-a476-4af1-b34e-11a4768af103',
    roles: [
      {
        id: '90d39584-e4dc-482e-9395-84e4dcd82ea5',
        name: 'ADMIN',
        scope: 'ENVIRONMENT',
        permissions: {
          AUDIT: ['C', 'R', 'U', 'D'],
          QUALITY_RULE: ['C', 'R', 'U', 'D'],
          DOCUMENTATION: ['C', 'R', 'U', 'D'],
          API_HEADER: ['C', 'R', 'U', 'D'],
          APPLICATION: ['C', 'R', 'U', 'D'],
          THEME: ['C', 'R', 'U', 'D'],
          INSTANCE: ['C', 'R', 'U', 'D'],
          ALERT: ['C', 'R', 'U', 'D'],
          SETTINGS: ['C', 'R', 'U', 'D'],
          ENTRYPOINT: ['C', 'R', 'U', 'D'],
          TENANT: ['C', 'R', 'U', 'D'],
          TOP_APIS: ['C', 'R', 'U', 'D'],
          CLIENT_REGISTRATION_PROVIDER: ['C', 'R', 'U', 'D'],
          PLATFORM: ['C', 'R', 'U', 'D'],
          GROUP: ['C', 'R', 'U', 'D'],
          MESSAGE: ['C', 'R', 'U', 'D'],
          NOTIFICATION: ['C', 'R', 'U', 'D'],
          DASHBOARD: ['C', 'R', 'U', 'D'],
          CATEGORY: ['C', 'R', 'U', 'D'],
          DICTIONARY: ['C', 'R', 'U', 'D'],
          API: ['C', 'R', 'U', 'D'],
          TAG: ['C', 'R', 'U', 'D'],
          METADATA: ['C', 'R', 'U', 'D'],
          IDENTITY_PROVIDER_ACTIVATION: ['C', 'R', 'U', 'D'],
        },
      },
    ],
    envRoles: {
      DEFAULT: [
        {
          id: '90d39584-e4dc-482e-9395-84e4dcd82ea5',
          name: 'ADMIN',
          scope: 'ENVIRONMENT',
          permissions: {
            AUDIT: ['C', 'R', 'U', 'D'],
            QUALITY_RULE: ['C', 'R', 'U', 'D'],
            DOCUMENTATION: ['C', 'R', 'U', 'D'],
            API_HEADER: ['C', 'R', 'U', 'D'],
            APPLICATION: ['C', 'R', 'U', 'D'],
            THEME: ['C', 'R', 'U', 'D'],
            INSTANCE: ['C', 'R', 'U', 'D'],
            ALERT: ['C', 'R', 'U', 'D'],
            SETTINGS: ['C', 'R', 'U', 'D'],
            ENTRYPOINT: ['C', 'R', 'U', 'D'],
            TENANT: ['C', 'R', 'U', 'D'],
            TOP_APIS: ['C', 'R', 'U', 'D'],
            CLIENT_REGISTRATION_PROVIDER: ['C', 'R', 'U', 'D'],
            PLATFORM: ['C', 'R', 'U', 'D'],
            GROUP: ['C', 'R', 'U', 'D'],
            MESSAGE: ['C', 'R', 'U', 'D'],
            NOTIFICATION: ['C', 'R', 'U', 'D'],
            DASHBOARD: ['C', 'R', 'U', 'D'],
            CATEGORY: ['C', 'R', 'U', 'D'],
            DICTIONARY: ['C', 'R', 'U', 'D'],
            API: ['C', 'R', 'U', 'D'],
            TAG: ['C', 'R', 'U', 'D'],
            METADATA: ['C', 'R', 'U', 'D'],
            IDENTITY_PROVIDER_ACTIVATION: ['C', 'R', 'U', 'D'],
          },
        },
      ],
    },
    source: 'memory',
    displayName: 'Bruce Wayne',
    firstname: 'Bruce',
    lastname: 'Wayne',
    email: 'me@batman.com',
    picture: 'https://batman.com/photo.jpeg',
    sourceId: 'batman',
    customFields: {},
    lastConnectionAt: 1631017105654,
    firstConnectionAt: 1630373735403,
    status: 'ACTIVE',
    loginCount: 1094,
    newsletterSubscribed: false,
    created_at: 1630373733551,
    updated_at: 1631017105654,
    primary_owner: true,
    number_of_active_tokens: 1,
  };

  return { ...base, ...attributes };
}
