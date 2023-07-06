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

export enum UTMMedium {
  DEBUG_MODE = 'feature_debugmode_v2',
  DEBUG_MODE_V4 = 'feature_debugmode_v4',
  AUDIT_TRAIL_ORG = 'feature_audit_trail_console',
  AUDIT_TRAIL_ENV = 'feature_audit_trail_api_list',
  AUDIT_TRAIL_API = 'feature_audit_trail_portal',
  OPENID_CONNECT = 'plugin_openidconnect',
  DCR_REGISTRATION = 'feature_dcr',
  SHARDING_TAGS = 'feature_sharding_tags',
  CUSTOM_ROLES = 'feature_custom_roles',
  CONFLUENT_SCHEMA_REGISTRY = 'resource_confluent_schema_registry',
  POLICY_STUDIO_V2 = 'v2_policy_studio_policy',
  POLICY_STUDIO_V4 = 'v4_policy_studio_policy',
  GENERAL_DANGER_ZONE = 'general_danger_zone',
  API_CREATION_TRY_MESSAGE = 'api_creation_try_message',
  API_CREATION_MESSAGE_ENTRYPOINT = 'api_creation_message_entrypoint',
  API_CREATION_MESSAGE_ENTRYPOINT_CONFIG = 'api_creation_message_entrypoint_config',
  API_CREATION_MESSAGE_SUMMARY = 'api_creation_message_summary',
}

export type UTMSource = 'oss_apim';

export type UTMCampaign = 'oss_apim_to_ee_apim';

export class UTM {
  constructor(private readonly source: UTMSource, private readonly medium: UTMMedium, private readonly campaign: UTMCampaign) {}

  public buildURL(base: string): string {
    return base + `?utm_source=${this.source}&utm_medium=${this.medium}&utm_campaign=${this.campaign}`;
  }

  public static ossEnterpriseV4(medium: UTMMedium): UTM {
    return new UTM('oss_apim', medium, 'oss_apim_to_ee_apim');
  }
}

export const UTM_DATA: Record<UTMMedium, UTM> = {
  [UTMMedium.DEBUG_MODE]: UTM.ossEnterpriseV4(UTMMedium.DEBUG_MODE),
  [UTMMedium.DEBUG_MODE_V4]: UTM.ossEnterpriseV4(UTMMedium.DEBUG_MODE_V4),
  [UTMMedium.AUDIT_TRAIL_ORG]: UTM.ossEnterpriseV4(UTMMedium.AUDIT_TRAIL_ORG),
  [UTMMedium.AUDIT_TRAIL_ENV]: UTM.ossEnterpriseV4(UTMMedium.AUDIT_TRAIL_ENV),
  [UTMMedium.AUDIT_TRAIL_API]: UTM.ossEnterpriseV4(UTMMedium.AUDIT_TRAIL_API),
  [UTMMedium.OPENID_CONNECT]: UTM.ossEnterpriseV4(UTMMedium.OPENID_CONNECT),
  [UTMMedium.DCR_REGISTRATION]: UTM.ossEnterpriseV4(UTMMedium.DCR_REGISTRATION),
  [UTMMedium.SHARDING_TAGS]: UTM.ossEnterpriseV4(UTMMedium.SHARDING_TAGS),
  [UTMMedium.CUSTOM_ROLES]: UTM.ossEnterpriseV4(UTMMedium.CUSTOM_ROLES),
  [UTMMedium.CONFLUENT_SCHEMA_REGISTRY]: UTM.ossEnterpriseV4(UTMMedium.CONFLUENT_SCHEMA_REGISTRY),
  [UTMMedium.POLICY_STUDIO_V2]: UTM.ossEnterpriseV4(UTMMedium.POLICY_STUDIO_V2),
  [UTMMedium.POLICY_STUDIO_V4]: UTM.ossEnterpriseV4(UTMMedium.POLICY_STUDIO_V4),
  [UTMMedium.GENERAL_DANGER_ZONE]: UTM.ossEnterpriseV4(UTMMedium.GENERAL_DANGER_ZONE),
  [UTMMedium.API_CREATION_TRY_MESSAGE]: UTM.ossEnterpriseV4(UTMMedium.API_CREATION_TRY_MESSAGE),
  [UTMMedium.API_CREATION_MESSAGE_ENTRYPOINT]: UTM.ossEnterpriseV4(UTMMedium.API_CREATION_MESSAGE_ENTRYPOINT),
  [UTMMedium.API_CREATION_MESSAGE_ENTRYPOINT_CONFIG]: UTM.ossEnterpriseV4(UTMMedium.API_CREATION_MESSAGE_ENTRYPOINT_CONFIG),
  [UTMMedium.API_CREATION_MESSAGE_SUMMARY]: UTM.ossEnterpriseV4(UTMMedium.API_CREATION_MESSAGE_SUMMARY),
};
