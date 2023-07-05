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
}

export type UTMSource = 'oss_apim';

export type UTMCampaign = 'oss_apim_to_ee_apim';

export class UTM {
  constructor(private readonly source: UTMSource, private readonly medium: UTMMedium, private readonly campaign: UTMCampaign) {}

  public buildURL(base: string): string {
    return base + `?utm_source=${this.source}&utm_medium=${this.medium}&utm_campaign=${this.campaign}`;
  }
}

export const UTM_DATA: Record<UTMMedium, UTM> = {
  [UTMMedium.DEBUG_MODE]: new UTM('oss_apim', UTMMedium.DEBUG_MODE, 'oss_apim_to_ee_apim'),
  [UTMMedium.DEBUG_MODE_V4]: new UTM('oss_apim', UTMMedium.DEBUG_MODE_V4, 'oss_apim_to_ee_apim'),
  [UTMMedium.AUDIT_TRAIL_ORG]: new UTM('oss_apim', UTMMedium.AUDIT_TRAIL_ORG, 'oss_apim_to_ee_apim'),
  [UTMMedium.AUDIT_TRAIL_ENV]: new UTM('oss_apim', UTMMedium.AUDIT_TRAIL_ENV, 'oss_apim_to_ee_apim'),
  [UTMMedium.AUDIT_TRAIL_API]: new UTM('oss_apim', UTMMedium.AUDIT_TRAIL_API, 'oss_apim_to_ee_apim'),
  [UTMMedium.OPENID_CONNECT]: new UTM('oss_apim', UTMMedium.OPENID_CONNECT, 'oss_apim_to_ee_apim'),
  [UTMMedium.DCR_REGISTRATION]: new UTM('oss_apim', UTMMedium.DCR_REGISTRATION, 'oss_apim_to_ee_apim'),
  [UTMMedium.SHARDING_TAGS]: new UTM('oss_apim', UTMMedium.SHARDING_TAGS, 'oss_apim_to_ee_apim'),
  [UTMMedium.CUSTOM_ROLES]: new UTM('oss_apim', UTMMedium.CUSTOM_ROLES, 'oss_apim_to_ee_apim'),
  [UTMMedium.CONFLUENT_SCHEMA_REGISTRY]: new UTM('oss_apim', UTMMedium.CONFLUENT_SCHEMA_REGISTRY, 'oss_apim_to_ee_apim'),
  [UTMMedium.POLICY_STUDIO_V2]: new UTM('oss_apim', UTMMedium.POLICY_STUDIO_V2, 'oss_apim_to_ee_apim'),
  [UTMMedium.POLICY_STUDIO_V4]: new UTM('oss_apim', UTMMedium.POLICY_STUDIO_V4, 'oss_apim_to_ee_apim'),
};
