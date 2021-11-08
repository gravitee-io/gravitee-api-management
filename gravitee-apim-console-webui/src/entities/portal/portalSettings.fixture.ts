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
import { PortalSettings } from './portalSettings';

export function fakePortalSettings(attributes?: Partial<PortalSettings>): PortalSettings {
  const base: PortalSettings = {
    portal: {
      entrypoint: 'https://api.company.com',
      apikeyHeader: 'X-Gravitee-Api-Key',
      support: {
        enabled: true,
      },
      apis: {
        tilesMode: {
          enabled: true,
        },
        categoryMode: {
          enabled: true,
        },
        apiHeaderShowTags: {
          enabled: true,
        },
        apiHeaderShowCategories: {
          enabled: true,
        },
      },
      analytics: {
        enabled: false,
      },
      rating: {
        enabled: true,
        comment: {
          mandatory: false,
        },
      },
      userCreation: {
        enabled: true,
        automaticValidation: {
          enabled: true,
        },
      },
      uploadMedia: {
        enabled: false,
        maxSizeInOctet: 1000000,
      },
    },

    metadata: {
      readonly: [
        'email.enabled',
        'email.host',
        'email.port',
        'email.username',
        'email.password',
        'email.subject',
        'email.from',
        'email.properties.auth',
        'email.properties.starttls.enable',
        'http.api.portal.cors.allow-origin',
      ],
    },
  };

  return {
    ...base,
    ...attributes,
  };
}
