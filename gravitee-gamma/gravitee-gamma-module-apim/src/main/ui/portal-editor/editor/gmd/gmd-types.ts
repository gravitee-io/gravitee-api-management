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

/** Structural block shape used by the GMD layer (avoids importing schema here). */
export interface GammaPartialBlock {
    readonly type?: string;
    readonly props?: Record<string, unknown>;
    readonly children?: GammaPartialBlock[];
    readonly content?: unknown;
    readonly id?: string;
}

export type GammaBlock = GammaPartialBlock & { readonly type: string };
