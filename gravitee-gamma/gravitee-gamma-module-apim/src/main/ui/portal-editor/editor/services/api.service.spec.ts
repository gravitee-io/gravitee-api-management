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
import { searchApis } from './api.service';

describe('api.service', () => {
    it('should return mock APIs with pagination metadata', async () => {
        const response = await searchApis({ size: 3 });

        expect(response.data).toHaveLength(3);
        expect(response.metadata?.pagination?.total).toBe(10);
    });

    it('should filter APIs by search query', async () => {
        const response = await searchApis({ q: 'payments' });

        expect(response.data).toHaveLength(1);
        expect(response.data?.[0]?.name).toBe('Payments API');
    });

    it('should respect the size limit', async () => {
        const response = await searchApis({ size: 2 });

        expect(response.data).toHaveLength(2);
    });
});
