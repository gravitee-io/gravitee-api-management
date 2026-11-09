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
import { buildModuleNavPath } from '@gravitee/gamma-modules-sdk/routing';

import type { EnvironmentApiScore } from '../types/scoring';

type ScoreDetailTarget = { module: string; path: (encodedApiId: string) => string };

/** API types whose score page lives outside the APIM module; any other type opens in APIM. */
const SCORE_DETAIL_TARGETS: Partial<Record<string, ScoreDetailTarget>> = {
    MCP_PROXY: { module: 'aim', path: id => `mcp-proxy/${id}/api-score` },
};

const APIM_TARGET: ScoreDetailTarget = { module: 'apim', path: id => `apis/${id}/api-score` };

export function apiScoreDetailPath(currentPathname: string, api: Pick<EnvironmentApiScore, 'id' | 'type'>): string {
    const target = (api.type && SCORE_DETAIL_TARGETS[api.type]) || APIM_TARGET;
    return buildModuleNavPath(target.module, target.path(encodeURIComponent(api.id)), currentPathname);
}
