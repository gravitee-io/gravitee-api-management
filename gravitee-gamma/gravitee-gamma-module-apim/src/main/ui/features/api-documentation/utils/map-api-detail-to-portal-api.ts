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
import type { Api } from '@apim/portal-editor/editor/entities/api';

import type { ApiDetailDto } from '../../apis/types';

export function mapApiDetailToPortalApi(api: ApiDetailDto): Api {
    return {
        id: api.id,
        name: api.name,
        version: api.apiVersion ?? '—',
        type: api.type,
        definitionVersion: api.definitionVersion === 'V4_NATIVE' ? 'V4' : 'V4',
        description: api.description ?? '',
        entrypoints: [],
        labels: api.labels,
        categories: api.categories,
        owner: {
            id: api.primaryOwner?.id ?? '',
            displayName: api.primaryOwner?.displayName ?? '—',
        },
    };
}
