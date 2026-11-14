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
import { ApimApiError } from '../../../shared/api/apimClient';
import type { ApiResource } from '../../apis/types/resource';
import type { ApiProductResourcesApplyFn } from '../services/apiProduct';

function findResource(current: ApiResource[], resourceName: string): ApiResource {
    const target = current.find(r => r.name === resourceName);
    if (!target) {
        throw new ApimApiError(409, 'This resource was removed or changed. The list will refresh.');
    }
    return target;
}

export function toggleProductResourceApply(resourceName: string): ApiProductResourcesApplyFn {
    return current => {
        const target = findResource(current, resourceName);
        const enabled = target.enabled !== false;
        return current.map(r => (r.name === resourceName ? { ...r, enabled: !enabled } : r));
    };
}

export function removeProductResourceApply(resourceName: string): ApiProductResourcesApplyFn {
    return current => {
        findResource(current, resourceName);
        return current.filter(r => r.name !== resourceName);
    };
}

export function upsertProductResourceApply(resource: ApiResource, replaceName?: string): ApiProductResourcesApplyFn {
    return current => {
        if (replaceName) {
            findResource(current, replaceName);
            const duplicate = current.some(r => r.name === resource.name && r.name !== replaceName);
            if (duplicate) {
                throw new ApimApiError(400, 'A resource with this name already exists.');
            }
            return current.map(r => (r.name === replaceName ? resource : r));
        }
        if (current.some(r => r.name === resource.name)) {
            throw new ApimApiError(400, 'A resource with this name already exists.');
        }
        return [...current, resource];
    };
}
