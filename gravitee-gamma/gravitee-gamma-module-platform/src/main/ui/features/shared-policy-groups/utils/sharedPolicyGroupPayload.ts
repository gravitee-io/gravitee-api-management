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

import type { ApiType, FlowPhase } from '../types/sharedPolicyGroup';

/** Mirrors classic Console's `PHASE_BY_API_TYPE` (shared-policy-groups-add-edit-dialog.component.ts). */
export const PHASE_BY_API_TYPE: Record<ApiType, FlowPhase[]> = {
    PROXY: ['REQUEST', 'RESPONSE'],
    A2A_PROXY: ['REQUEST', 'RESPONSE'],
    LLM_PROXY: ['REQUEST', 'RESPONSE'],
    MCP_PROXY: ['REQUEST', 'RESPONSE'],
    MESSAGE: ['REQUEST', 'RESPONSE', 'PUBLISH', 'SUBSCRIBE'],
    NATIVE: ['PUBLISH', 'SUBSCRIBE', 'ENTRYPOINT_CONNECT', 'INTERACT'],
};
