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
import { FeatureInfo } from '@gravitee/ui-particles-angular';

import { CTAConfiguration } from '../../../entities/management-api-v2/consoleCustomization';

export enum ApimFeature {
  APIM_CUSTOM_ROLES = 'apim-custom-roles',
  APIM_OPENID_CONNECT_SSO = 'apim-openid-connect-sso',
  APIM_SHARDING_TAGS = 'apim-sharding-tags',
  APIM_AUDIT_TRAIL = 'apim-audit-trail',
  APIM_DEBUG_MODE = 'apim-debug-mode',
  APIM_DCR_REGISTRATION = 'apim-dcr-registration',
  APIM_NATIVE_KAFKA_REACTOR = 'apim-native-kafka-reactor',
  APIM_POLICY_V2 = 'apim-policy-v2',
  APIM_SCHEMA_REGISTRY_PROVIDER = 'apim-en-schema-registry-provider',
  APIM_EN_MESSAGE_REACTOR = 'apim-en-message-reactor',
  APIM_LLM_PROXY_REACTOR = 'apim-llm-proxy-reactor',
  APIM_CLUSTER = 'apim-cluster',
  APIM_API_PRODUCTS = 'apim-api-products',
  ALERT_ENGINE = 'alert-engine',
  FEDERATION = 'federation',
}

export enum UTMTags {
  CONTEXT_API = 'api',
  CONTEXT_API_ANALYTICS = 'api_analytics',
  CONTEXT_API_NOTIFICATIONS = 'api_notifications',
  CONTEXT_API_V2 = 'api_v2',
  CONTEXT_API_V4 = 'api_v4',
  CONTEXT_ENVIRONMENT = 'environment',
  CONTEXT_ORGANIZATION = 'organization',
  API_CONFLUENT = 'api_confluent',
  GENERAL_DANGER_ZONE = 'general_danger_zone',
  GENERAL_ENDPOINT_CONFIG = 'general_endpoint_config',
  GENERAL_ENTRYPOINT_CONFIG = 'general_entrypoint_config',
  API_CREATION_TRY_MESSAGE = 'api_creation_try_message',
  API_CREATION_MESSAGE_ENTRYPOINT = 'api_creation_message_entrypoint',
  API_CREATION_LLM_ENTRYPOINT = 'api_creation_llm_entrypoint',
  API_CREATION_MESSAGE_ENTRYPOINT_CONFIG = 'api_creation_message_entrypoint_config',
  API_CREATION_NATIVE_KAFKA_ENTRYPOINT_CONFIG = 'api_creation_native_kafka_entrypoint_config',
  API_CREATION_LLM_ENTRYPOINT_CONFIG = 'api_creation_llm_entrypoint_config',
  API_CREATION_MESSAGE_ENDPOINT = 'api_creation_message_endpoint',
  API_CREATION_MESSAGE_ENDPOINT_CONFIG = 'api_creation_message_endpoint_config',
  API_CREATION_LLM_ENDPOINT_CONFIG = 'api_creation_llm_endpoint_config',
  API_CREATION_NATIVE_KAFKA_ENDPOINT_CONFIG = 'api_creation_native_kafka_endpoint_config',
  API_CREATION_MESSAGE_SUMMARY = 'api_creation_message_summary',
  API_CREATION_LLM_SUMMARY = 'api_creation_llm_summary',
}

export function stringFeature(ctaConfig: CTAConfiguration, value: string): ApimFeature {
  const feature = value as ApimFeature;
  if (getFeatureInfoData(ctaConfig)[feature]) {
    return feature;
  }
  throw new Error(`Unknown Feature value ${value}. Expected one of ${Object.keys(getFeatureInfoData)}`);
}

export const getFeatureInfoData = (ctaConfig: CTAConfiguration): Record<ApimFeature, FeatureInfo> => {
  const title = ctaConfig?.title || 'Unlock Gravitee Enterprise';
  const ee = ctaConfig?.customEnterpriseName || 'Gravitee Enterprise';
  const trialButtonLabel = ctaConfig?.trialButtonLabel || 'Request an enterprise license';
  const hideDays = ctaConfig?.hideDays || false;

  return {
    [ApimFeature.FEDERATION]: {
      title,
      image: 'assets/gio-ee-unlock-dialog/policies.svg',
      description: `Request a license to unlock enterprise functionality, such as support for event-native APIs, multitenancy support, enterprise policies, and federation of 3rd-party gateways and brokers.`,
      trialButtonLabel,
      hideDays,
    },
    [ApimFeature.APIM_NATIVE_KAFKA_REACTOR]: {
      title,
      image: 'assets/gio-ee-unlock-dialog/policies.svg',
      description: `Request the Native Kafka pack to unlock Kafka APIs and the Kafka Gateway.`,
      trialButtonLabel,
      hideDays,
    },
    [ApimFeature.APIM_LLM_PROXY_REACTOR]: {
      title,
      image: 'assets/gio-ee-unlock-dialog/policies-llm.svg',
      description: `Request the Agent Mesh pack to unlock the LLM proxy APIs.`,
      trialButtonLabel,
      hideDays,
    },
    [ApimFeature.APIM_CUSTOM_ROLES]: {
      title,
      image: 'assets/gio-ee-unlock-dialog/roles-customisation.svg',
      description: `Custom Roles is part of ${ee}. Custom Roles allows you to specify a wide range of permissions applied to different scopes, which can then be assigned to groups and users.`,
      trialButtonLabel,
      hideDays,
    },
    [ApimFeature.APIM_OPENID_CONNECT_SSO]: {
      title,
      image: 'assets/gio-ee-unlock-dialog/openid-connect.svg',
      description: `OpenID Connect is part of ${ee}. The OpenID Connect Provider allows users to authenticate using third-party providers like Okta, Keycloak and Ping.`,
      trialButtonLabel,
      hideDays,
    },
    [ApimFeature.APIM_SHARDING_TAGS]: {
      title,
      image: 'assets/gio-ee-unlock-dialog/sharding-tags.svg',
      description: `Sharding Tags is part of ${ee}. Sharding Tags allows you to federate across multiple Gateway deployments, and control which APIs should be deployed where, and by which groups.`,
      trialButtonLabel,
      hideDays,
    },
    [ApimFeature.APIM_AUDIT_TRAIL]: {
      title,
      image: 'assets/gio-ee-unlock-dialog/audit-trail.svg',
      description: `Audit is part of ${ee}. Audit gives you a complete understanding of events and their context to strengthen your security posture.`,
      trialButtonLabel,
      hideDays,
    },
    [ApimFeature.APIM_DEBUG_MODE]: {
      title,
      image: 'assets/gio-ee-unlock-dialog/debug-mode.svg',
      description: `Debug Mode is part of ${ee}. It provides detailed information about the behaviour of each policy in your flows and trace attributes and data values across execution.`,
      trialButtonLabel,
      hideDays,
    },
    [ApimFeature.APIM_DCR_REGISTRATION]: {
      title,
      image: 'assets/gio-ee-unlock-dialog/dcr-providers.svg',
      description: `Dynamic Client Registration (DCR) Provider is part of ${ee}. DCR enhances your API's security by seamlessly integrating OAuth 2.0 and OpenID Connect.`,
      trialButtonLabel,
      hideDays,
    },
    [ApimFeature.APIM_POLICY_V2]: {
      title,
      image: 'assets/gio-ee-unlock-dialog/policies.svg',
      description: `This policy is part of ${ee}. Enterprise policies allows you to easily define and customise rules according to your evolving business needs.`,
      trialButtonLabel,
      hideDays,
    },
    [ApimFeature.APIM_SCHEMA_REGISTRY_PROVIDER]: {
      title,
      image: 'assets/gio-ee-unlock-dialog/confluent-schema-registry.svg',
      description: `Confluent Schema Registry is part of ${ee}. Integration with a Schema Registry enables your APIs to validate schemas used in API calls, and serialize and deserialize data.`,
      trialButtonLabel,
      hideDays,
    },
    [ApimFeature.APIM_EN_MESSAGE_REACTOR]: {
      title: 'Request an upgrade', // FIXME: should we keep this custom title here?
      image: 'assets/gio-ee-unlock-dialog/ee-upgrade.svg',
      description: `Explore ${ee} functionality, such as support for event brokers, asynchronous APIs, and Webhook subscriptions.`,
      trialButtonLabel,
      hideDays,
    },
    [ApimFeature.ALERT_ENGINE]: {
      title,
      image: 'assets/gio-ee-unlock-dialog/alert-engine.svg',
      description: `Alert Engine allows you to isolate, understand and remediate for API performance and security risks before they cause a problem for your customers.`,
      trialButtonLabel,
      hideDays,
    },
    [ApimFeature.APIM_CLUSTER]: {
      title,
      image: 'assets/gio-ee-unlock-dialog/clusters.svg',
      description: `Cluster is part of ${ee}. Accelerate and standardize event-driven development by allowing you to configure, manage permissions for, and reuse your event-broker clusters as centrally-governed assets.`,
      trialButtonLabel,
      hideDays,
    },
    [ApimFeature.APIM_API_PRODUCTS]: {
      title,
      image: 'assets/gio-ee-unlock-dialog/api-products.svg',
      description: `API Products is part of ${ee}. An API Product provides unified access to multiple APIs through one subscription.`,
      trialButtonLabel,
      hideDays,
    },
  };
};
