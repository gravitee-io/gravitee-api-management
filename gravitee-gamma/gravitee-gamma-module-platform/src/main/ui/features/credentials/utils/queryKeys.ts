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
export const credentialKeys = {
    all: ['environment-credentials'] as const,
    lists: () => [...credentialKeys.all, 'list'] as const,
    list: (envId: string, query: string, page: number, perPage: number) =>
        [...credentialKeys.lists(), envId, query, page, perPage] as const,
    detail: (envId: string, credentialId: string) => [...credentialKeys.all, 'detail', envId, credentialId] as const,
    consumers: (envId: string, credentialId: string) => [...credentialKeys.all, 'consumers', envId, credentialId] as const,
} as const;
