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
import { mapApiDetailToPortalApi } from './map-api-detail-to-portal-api';

describe('mapApiDetailToPortalApi', () => {
    it('should map API detail fields used by metadata blocks', () => {
        expect(
            mapApiDetailToPortalApi({
                id: 'api-1',
                name: 'Sports API',
                apiVersion: '2.1.0',
                description: 'Manage sports data.',
                labels: ['public'],
                primaryOwner: { id: 'owner-1', displayName: 'Platform Team' },
            }),
        ).toEqual({
            id: 'api-1',
            name: 'Sports API',
            version: '2.1.0',
            type: undefined,
            definitionVersion: 'V4',
            description: 'Manage sports data.',
            entrypoints: [],
            labels: ['public'],
            categories: undefined,
            owner: { id: 'owner-1', displayName: 'Platform Team' },
        });
    });
});
