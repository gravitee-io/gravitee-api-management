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
import { PortalConfiguration, PortalSettings } from './portalSettings';

export function fakePortalSettings(attributes?: Partial<PortalSettings>): PortalSettings {
  const base: PortalSettings = {
    ...fakePortalConfiguration(),
    cors: {
      allowOrigin: ['test.entrypoint.dev', 'test.entrypoint.dev2'],
      allowHeaders: ['Cache-Control', 'Pragma'],
      allowMethods: ['GET', 'DELETE'],
      exposedHeaders: ['ETag', 'X-Xsrf-Token'],
      maxAge: 1728000,
    },
  };

  return {
    ...base,
    ...attributes,
  };
}

export function fakePortalConfiguration(attributes?: Partial<PortalConfiguration>): PortalConfiguration {
  const base: PortalConfiguration = {
    portal: {
      entrypoint: 'https://api.company.com',
      apikeyHeader: 'X-Gravitee-Api-Key',
      kafkaDomain: '',
      kafkaPort: 9092,
      kafkaSaslMechanisms: ['PLAIN', 'SCRAM-SHA-256', 'SCRAM-SHA-512'],
      homepageTitle: '',
      support: {
        enabled: true,
      },
      apis: {
        tilesMode: {
          enabled: true,
        },
        documentationOnlyMode: {
          enabled: false,
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
        promotedApiMode: {
          enabled: true,
        },
      },
      analytics: {
        enabled: false,
        trackingId: '',
      },
      rating: {
        enabled: true,
        comment: {
          mandatory: false,
        },
      },
      url: '',
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
    email: {
      enabled: true,
      host: 'testhost',
      port: 1,
      username: 'testusername',
      password: '',
      protocol: 'smtp',
      subject: '[gravitee] %s',
      from: 'test@email',
      properties: {
        auth: true,
        startTlsEnable: false,
        sslTrust: 'testssl',
      },
    },
    api: {
      labelsDictionary: ['test'],
      primaryOwnerMode: 'USER',
    },
    apiQualityMetrics: {
      enabled: false,
      functionalDocumentationWeight: 1041,
      technicalDocumentationWeight: 1052,
      descriptionWeight: 2412,
      descriptionMinLength: 1022,
      logoWeight: 1033,
      categoriesWeight: 1040,
      labelsWeight: 1041,
      healthcheckWeight: 2,
    },
    apiReview: {
      enabled: false,
    },
    application: {
      registration: {
        enabled: false,
      },
      types: {
        simple: {
          enabled: true,
        },
        browser: {
          enabled: true,
        },
        web: {
          enabled: true,
        },
        native: {
          enabled: true,
        },
        backend_to_backend: {
          enabled: true,
        },
      },
    },
    company: {
      name: 'Gravitee22',
    },
    documentation: {
      url: 'https://docs.gravitee.ios',
      pageNotFoundMessage: null,
    },
    openAPIDocViewer: {
      openAPIDocType: {
        swagger: {
          enabled: false,
        },
        redoc: {
          enabled: true,
        },
        defaultType: 'Redoc',
      },
    },
    plan: {
      security: {
        apikey: {
          enabled: true,
        },
        customApiKey: {
          enabled: true,
        },
        sharedApiKey: {
          enabled: false,
        },
        oauth2: {
          enabled: true,
        },
        keyless: {
          enabled: false,
        },
        jwt: {
          enabled: true,
        },
        push: {
          enabled: true,
        },
        mtls: {
          enabled: true,
        },
      },
    },
    scheduler: {
      tasks: 10,
      notifications: '101',
    },
    dashboards: {
      apiStatus: {
        enabled: true,
      },
    },
    portalNext: {
      access: {
        enabled: true,
      },
      banner: {
        enabled: true,
        title: 'testTitle',
        subtitle: 'testSubtitle',
        primaryButton: {
          enabled: false,
        },
        secondaryButton: {
          enabled: false,
        },
      },
    },
    logging: {
      maxDurationMillis: 1000,
      audit: {
        enabled: true,
        trail: {
          enabled: true,
        },
      },
      user: {
        displayed: true,
      },
      messageSampling: {
        probabilistic: {
          default: 0.01,
          limit: 0.5,
        },
        count: {
          default: 10,
          limit: 100,
        },
        temporal: {
          default: 'PT1S',
          limit: 'PT1S',
        },
      },
    },
  };

  return {
    ...base,
    ...attributes,
  };
}
