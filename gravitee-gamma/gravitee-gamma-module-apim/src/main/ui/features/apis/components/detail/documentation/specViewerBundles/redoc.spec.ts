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
import { createRedocStore } from './redoc';

const OPTIONS = { disableSearch: true };

const SWAGGER_2 = {
    swagger: '2.0',
    info: { title: 'Petstore', version: '1.0.0' },
    paths: { '/pets': { get: { responses: { 200: { description: 'The pets' } } } } },
};

describe('createRedocStore', () => {
    let scrollListeners: number;

    beforeEach(() => {
        scrollListeners = 0;
        jest.spyOn(window, 'addEventListener').mockImplementation(type => {
            if (type === 'scroll') scrollListeners++;
        });
        jest.spyOn(window, 'removeEventListener').mockImplementation(type => {
            if (type === 'scroll') scrollListeners--;
        });
        jest.spyOn(console, 'warn').mockImplementation(() => undefined);
    });

    afterEach(() => {
        jest.restoreAllMocks();
    });

    it('reads a Swagger 2.0 document, as the portal does', async () => {
        const store = await createRedocStore(SWAGGER_2, OPTIONS);

        expect(store.spec.info.title).toBe('Petstore');
        expect(store.menu.flatItems.map(item => item.name)).toContain('/pets');
        store.dispose();
    });

    it('releases its window listener once disposed', async () => {
        const store = await createRedocStore(SWAGGER_2, OPTIONS);
        expect(scrollListeners).toBe(1);

        store.dispose();

        expect(scrollListeners).toBe(0);
    });

    it.each([
        ['without info', { openapi: '3.0.3', paths: {} }],
        ['with tags that are not a list', { openapi: '3.0.3', info: { title: 'x', version: '1' }, tags: 'pets', paths: {} }],
        ['with tag groups that are not a list', { openapi: '3.0.3', info: { title: 'x', version: '1' }, 'x-tagGroups': 'pets', paths: {} }],
    ])('leaves no window listener behind when Redoc cannot read a document %s', async (_case, document) => {
        await expect(createRedocStore(document, OPTIONS)).rejects.toThrow();

        expect(scrollListeners).toBe(0);
    });
});
