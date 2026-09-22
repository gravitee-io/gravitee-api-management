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
import type { ApiDetailDto, ApiListItem } from '../types';

export function isFederatedApi(api: ApiDetailDto | null | undefined): boolean {
    return api?.definitionVersion === 'FEDERATED';
}

export function isFederatedAgentApi(api: ApiDetailDto | null | undefined): boolean {
    return api?.definitionVersion === 'FEDERATED_AGENT';
}

// Keyed on the row's own `definitionVersion` rather than on `originContext.origin`, which a
// FEDERATED_AGENT row also carries as INTEGRATION and which therefore cannot tell the two apart.
export function isFederatedApiListItem(api: ApiListItem): boolean {
    switch (api.definitionVersion) {
        case 'FEDERATED':
            return true;
        case 'FEDERATED_AGENT':
        case 'V4':
        case 'V2':
            return false;
        default: {
            const unhandled: never = api.definitionVersion;
            return unhandled;
        }
    }
}
