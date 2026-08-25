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

/**
 * The whole organization, as `GET /organizations/{orgId}` serves it: `OrganizationEntity` in
 * `io.gravitee.rest.api.model`, field for field and in the same order. The organization is saved by a
 * full-entity PUT, so a field missing here would be dropped from the organization on the next save.
 * Only `id` and `name` are guaranteed: the mapper omits nulls.
 */
export interface Organization {
    id: string;
    cockpitId?: string;
    hrids?: string[];
    name: string;
    description?: string;
    flowMode?: PlatformFlowMode;
    flows?: PlatformFlow[];
}

export type PlatformFlowMode = 'DEFAULT' | 'BEST_MATCH';

/** Sharding tag a platform flow is restricted to. The organization entity stores tag ids, not keys. */
export interface PlatformFlowConsumer {
    consumerType: 'TAG';
    consumerId: string;
}

export interface PlatformFlowPathOperator {
    path?: string;
    operator?: 'EQUALS' | 'STARTS_WITH';
}

export interface PlatformFlowStep {
    name?: string;
    policy?: string;
    description?: string;
    configuration?: unknown;
    enabled?: boolean;
    condition?: string;
}

/** Platform flow as the organization entity stores it: the v2 wire format, with `pre`/`post` and a hyphenated path operator. */
export interface PlatformFlow {
    id?: string;
    name?: string;
    'path-operator'?: PlatformFlowPathOperator;
    pre?: PlatformFlowStep[];
    post?: PlatformFlowStep[];
    enabled?: boolean;
    methods?: string[];
    condition?: string;
    consumers?: PlatformFlowConsumer[];
}
