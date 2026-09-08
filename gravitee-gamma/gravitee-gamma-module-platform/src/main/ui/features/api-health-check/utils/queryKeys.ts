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

export const environmentHealthKeys = {
    all: ['environment-health'] as const,
    search: (envId: string, query: string, page: number, perPage: number, sortBy: string | undefined, reloadToken: number) =>
        [...environmentHealthKeys.all, 'search', envId, query, page, perPage, sortBy, reloadToken] as const,
    report: (envId: string, from: number, to: number, reloadToken: number) =>
        [...environmentHealthKeys.all, 'report', envId, from, to, reloadToken] as const,
    availability: (envId: string, apiId: string, from: number, to: number, reloadToken: number) =>
        [...environmentHealthKeys.all, 'availability', envId, apiId, from, to, reloadToken] as const,
} as const;
