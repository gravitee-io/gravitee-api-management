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

export const entrypointKeys = {
    all: ['entrypoints'] as const,
    list: () => [...entrypointKeys.all, 'list'] as const,
} as const;

export const orgEnvironmentKeys = {
    all: ['org-environments'] as const,
    list: () => [...orgEnvironmentKeys.all, 'list'] as const,
} as const;

export const portalSettingsKeys = {
    all: ['portal-settings'] as const,
    byEnvironment: (envId: string) => [...portalSettingsKeys.all, envId] as const,
} as const;

export const orgTagKeys = {
    all: ['org-tags'] as const,
    list: () => [...orgTagKeys.all, 'list'] as const,
} as const;

export const orgGroupKeys = {
    all: ['org-groups'] as const,
    list: () => [...orgGroupKeys.all, 'list'] as const,
} as const;
