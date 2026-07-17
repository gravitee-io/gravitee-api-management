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
import { getApiProductById, searchApiProducts } from './api-product.service';

describe('api-product.service', () => {
    it('should return a product by id', async () => {
        const product = await getApiProductById('product-commerce');
        expect(product).toMatchObject({
            id: 'product-commerce',
            name: 'Commerce Platform',
            apiIds: expect.arrayContaining(['api-payments', 'api-orders']),
        });
    });

    it('should return undefined for unknown product id', async () => {
        expect(await getApiProductById('unknown-product')).toBeUndefined();
    });

    it('should search products by name', async () => {
        const response = await searchApiProducts({ q: 'finance' });
        expect(response.data).toHaveLength(1);
        expect(response.data?.[0].id).toBe('product-finance');
    });

    it('should search products by description', async () => {
        const response = await searchApiProducts({ q: 'customer-facing' });
        expect(response.data).toHaveLength(1);
        expect(response.data?.[0].id).toBe('product-customer');
    });
});
