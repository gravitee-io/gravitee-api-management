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

export type DictionaryType = 'MANUAL' | 'DYNAMIC';

export type DictionaryLifecycleState = 'STARTED' | 'STOPPED';

/** Jackson `TimeUnit` enum names used by Management API. */
export type DictionaryTriggerUnit = 'NANOSECONDS' | 'MICROSECONDS' | 'MILLISECONDS' | 'SECONDS' | 'MINUTES' | 'HOURS' | 'DAYS';

export interface DictionaryTrigger {
    rate: number;
    unit: DictionaryTriggerUnit;
}

export interface DictionaryHttpProviderConfiguration {
    url?: string;
    method?: string;
    headers?: Array<{ name?: string; value?: string }>;
    body?: string;
    specification?: string;
    useSystemProxy?: boolean;
}

export interface DictionaryProvider {
    type: string;
    configuration: DictionaryHttpProviderConfiguration | Record<string, unknown>;
}

/**
 * List item from GET /configuration/dictionaries.
 * Note: `properties` is a count; `provider` is the provider type string.
 */
export interface DictionaryListItem {
    id: string;
    /** Optional API field used by classic Console list as ID display (`key || id`). Not a create/edit form field. */
    key?: string;
    name: string;
    description?: string;
    type: DictionaryType;
    state?: DictionaryLifecycleState;
    provider?: string;
    properties?: number;
    created_at?: string | number;
    updated_at?: string | number;
    deployed_at?: string | number;
}

/**
 * Detail entity from GET/POST/PUT /configuration/dictionaries/{id}.
 * Note: `properties` is the property key→value map; `provider` is the full object.
 * Create/update forms match classic Console: name, description, type (+ properties/provider/trigger) — no dictionary `key`.
 */
export interface Dictionary {
    id: string;
    /** Optional API field; not part of create/update UI (classic Console does not expose it). */
    key?: string;
    name: string;
    description?: string;
    type: DictionaryType;
    state?: DictionaryLifecycleState;
    properties?: Record<string, string>;
    provider?: DictionaryProvider | null;
    trigger?: DictionaryTrigger | null;
    created_at?: string | number;
    updated_at?: string | number;
    deployed_at?: string | number;
}

export interface NewDictionaryPayload {
    name: string;
    description?: string;
    type: DictionaryType;
    properties?: Record<string, string>;
    provider?: DictionaryProvider;
    trigger?: DictionaryTrigger;
}

export interface UpdateDictionaryPayload {
    name: string;
    description?: string;
    type: DictionaryType;
    properties?: Record<string, string>;
    provider?: DictionaryProvider;
    trigger?: DictionaryTrigger;
}

export type DictionaryLifecycleAction = 'START' | 'STOP';
