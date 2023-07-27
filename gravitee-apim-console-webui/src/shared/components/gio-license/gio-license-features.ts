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
export enum ApimFeature {
  APIM_CUSTOM_ROLES = 'apim-custom-roles',
  APIM_OPENID_CONNECT_SSO = 'apim-openid-connect-sso',
  APIM_SHARDING_TAGS = 'apim-sharding-tags',
  APIM_AUDIT_TRAIL = 'apim-audit-trail',
  APIM_DEBUG_MODE = 'apim-debug-mode',
  APIM_DCR_REGISTRATION = 'apim-dcr-registration',
  APIM_POLICY_V2 = 'apim-policy-v2',
  APIM_SCHEMA_REGISTRY_PROVIDER = 'apim-en-schema-registry-provider',
  APIM_EN_MESSAGE_REACTOR = 'apim-en-message-reactor',
}

export function stringFeature(value: string): ApimFeature {
  const feature = value as ApimFeature;
  if (FeatureInfoData[feature]) {
    return feature;
  }
  throw new Error(`Unknown Feature value ${value}. Expected one of ${Object.keys(FeatureInfoData)}`);
}

export interface FeatureInfo {
  image: string;
  description: string;
  title?: string;
}

export const FeatureInfoData: Record<ApimFeature, FeatureInfo> = {
  [ApimFeature.APIM_CUSTOM_ROLES]: {
    image: 'assets/gio-ee-unlock-dialog/roles-customisation.svg',
    description:
      'Custom Roles is part of Gravitee Enterprise. Custom Roles allows you to specify a wide range of permissions applied to different scopes, which can then be assigned to groups and users.',
  },
  [ApimFeature.APIM_OPENID_CONNECT_SSO]: {
    image: 'assets/gio-ee-unlock-dialog/openid-connect.svg',
    description:
      'OpenID Connect is part of Gravitee Enterprise. The OpenID Connect Provider allows users to authenticate to Gravitee using third-party providers like Okta, Keycloak and Ping.',
  },
  [ApimFeature.APIM_SHARDING_TAGS]: {
    image: 'assets/gio-ee-unlock-dialog/sharding-tags.svg',
    description:
      'Sharding Tags is part of Gravitee Enterprise. Sharding Tags allows you to federate across multiple Gateway deployments, and control which APIs should be deployed where, and by which groups.',
  },
  [ApimFeature.APIM_AUDIT_TRAIL]: {
    image: 'assets/gio-ee-unlock-dialog/audit-trail.svg',
    description:
      'Audit is part of Gravitee Enterprise. Audit gives you a complete understanding of events and their context to strengthen your security posture.',
  },
  [ApimFeature.APIM_DEBUG_MODE]: {
    image: 'assets/gio-ee-unlock-dialog/debug-mode.svg',
    description:
      'Debug Mode is part of Gravitee Enterprise. It provides detailed information about the behaviour of each policy in your flows and trace attributes and data values across execution.',
  },
  [ApimFeature.APIM_DCR_REGISTRATION]: {
    image: 'assets/gio-ee-unlock-dialog/dcr-providers.svg',
    description:
      "Dynamic Client Registration (DCR) Provider is part of Gravitee Enterprise. DCR enhances your API's security by seamlessly integrating OAuth 2.0 and OpenID Connect.",
  },
  [ApimFeature.APIM_POLICY_V2]: {
    image: 'assets/gio-ee-unlock-dialog/policies.svg',
    description:
      'This policy is part of Gravitee Enterprise. Enterprise policies allows you to easily define and customise rules according to your evolving business needs.',
  },
  [ApimFeature.APIM_SCHEMA_REGISTRY_PROVIDER]: {
    image: 'assets/gio-ee-unlock-dialog/confluent-schema-registry.svg',
    description:
      'Confluent Schema Registry is part of Gravitee Enterprise. Integration with a Schema Registry enables your APIs to validate schemas used in API calls, and serialize and deserialize data.',
  },
  [ApimFeature.APIM_EN_MESSAGE_REACTOR]: {
    title: 'Request an upgrade',
    image: 'assets/gio-ee-unlock-dialog/ee-upgrade.svg',
    description:
      'Explore Gravitee enterprise functionality, such as support for event brokers, asynchronous APIs, and Webhook subscriptions.',
  },
};
