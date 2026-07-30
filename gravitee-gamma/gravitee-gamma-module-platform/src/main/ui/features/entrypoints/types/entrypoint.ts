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

export type EntrypointTarget = 'HTTP' | 'TCP' | 'KAFKA';

export interface Entrypoint {
    id: string;
    value?: string;
    tags?: string[];
    environmentIds?: string[];
    target?: EntrypointTarget;
}

export interface OrgEnvironment {
    id: string;
    name: string;
    hrids?: string[];
}

export interface EntrypointPortalSettings {
    portal?: {
        entrypoint?: string;
        tcpPort?: number;
        kafkaDomain?: string;
        kafkaPort?: number;
        [key: string]: unknown;
    };
    metadata?: {
        readonly?: string[];
    };
    /** Full portal-settings document is preserved for round-trip save. */
    [key: string]: unknown;
}

export interface OrgTag {
    id: string;
    key: string;
    name: string;
    description?: string;
    restricted_groups?: string[];
}

export interface NewOrgTagPayload {
    name: string;
    key: string;
    description?: string;
    restricted_groups?: string[];
}

export interface UpdateOrgTagPayload {
    name: string;
    description?: string;
    restricted_groups?: string[];
}

export interface OrgGroup {
    id: string;
    name: string;
}

export interface ShardingTagRow {
    id: string;
    key: string;
    name: string;
    description: string;
    restrictedGroupIds: string[];
    restrictedGroupNames: string[];
}

export interface EnvironmentEntrypointConfig {
    environment: OrgEnvironment;
    portalSettings: EntrypointPortalSettings;
}

export interface EntrypointMappingRow {
    id: string;
    value: string;
    target: EntrypointTarget;
    targetLabel: string;
    tags: string[];
    tagsName: string[];
    environmentIds: string[];
    environmentNames: string[];
}

export interface NewEntrypointPayload {
    target: EntrypointTarget;
    value: string;
    tags: string[];
    environmentIds: string[];
}

export interface UpdateEntrypointPayload {
    id: string;
    target: EntrypointTarget;
    value: string;
    tags: string[];
    environmentIds: string[];
}
