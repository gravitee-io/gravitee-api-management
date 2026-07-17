/*
 * Copyright (C) 2026 The Gravitee team (http://gravitee.io)
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
export interface RemoteSpecSourceOptions {
    readonly useSystemProxy?: boolean;
    readonly autoFetch?: boolean;
    readonly lastSyncedAt?: number;
}

export interface GitSpecSourceFields {
    readonly repositoryUrl: string;
    readonly branch: string;
    readonly filepath: string;
}

export type OpenApiSpecSource =
    | { readonly type: 'API'; readonly apiId: string }
    | ({ readonly type: 'HTTP'; readonly url: string } & RemoteSpecSourceOptions)
    | ({ readonly type: 'GITHUB' | 'GITLAB' } & GitSpecSourceFields & RemoteSpecSourceOptions)
    | { readonly type: 'INLINE'; readonly content: string };

export type AsyncApiSpecSource =
    | { readonly type: 'API'; readonly apiId: string }
    | ({ readonly type: 'HTTP'; readonly url: string } & RemoteSpecSourceOptions)
    | ({ readonly type: 'GITHUB' | 'GITLAB' } & GitSpecSourceFields & RemoteSpecSourceOptions)
    | { readonly type: 'INLINE'; readonly content: string };
