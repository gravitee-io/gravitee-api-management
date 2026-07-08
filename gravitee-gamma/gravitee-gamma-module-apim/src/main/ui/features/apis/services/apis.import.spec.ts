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
import { http, HttpResponse } from 'msw';

import { updateApiFromDefinition, updateApiFromDefinitionUrl, updateApiFromSwagger } from './apis';
import { resetApimClientForTests } from '../../../shared/api/apimClient';
import { TEST_V2_BASE } from '../../../testing/factories';
import { trackHandler } from '../../../testing/helpers';
import { server } from '../../../testing/server';

const DEFINITION_PATH = `${TEST_V2_BASE}/apis/:apiId/_import/definition`;
const DEFINITION_URL_PATH = `${TEST_V2_BASE}/apis/:apiId/_import/definition-url`;
const SWAGGER_PATH = `${TEST_V2_BASE}/apis/:apiId/_import/swagger`;

describe('apis import actions', () => {
    beforeEach(() => {
        resetApimClientForTests();
    });

    it('updateApiFromDefinition PUTs the definition as JSON to _import/definition', async () => {
        const tracker = trackHandler('put', DEFINITION_PATH, { id: 'api-1', name: 'My API' });
        const definition = { api: { name: 'My API' } };

        const result = await updateApiFromDefinition('DEFAULT', 'api-1', definition);

        expect(tracker.callCount).toBe(1);
        expect(new URL(tracker.lastCall!.url).pathname).toContain('/apis/api-1/_import/definition');
        expect(tracker.lastCall?.method).toBe('PUT');
        expect(tracker.lastCall?.headers.get('content-type')).toContain('application/json');
        expect(tracker.lastCall?.body).toEqual(definition);
        expect(result).toEqual({ id: 'api-1', name: 'My API' });
    });

    it('updateApiFromDefinitionUrl PUTs the raw URL as text/plain to _import/definition-url', async () => {
        const requests: { url: string; contentType: string | null; body: string }[] = [];
        server.use(
            http.put(DEFINITION_URL_PATH, async ({ request }) => {
                requests.push({
                    url: request.url,
                    contentType: request.headers.get('content-type'),
                    body: await request.text(),
                });
                return HttpResponse.json({ id: 'api-1', name: 'My API' });
            }),
        );

        const result = await updateApiFromDefinitionUrl('DEFAULT', 'api-1', 'https://example.com/api-definition.json');

        expect(requests).toHaveLength(1);
        expect(new URL(requests[0].url).pathname).toContain('/apis/api-1/_import/definition-url');
        expect(requests[0].contentType).toBe('text/plain');
        expect(requests[0].body).toBe('https://example.com/api-definition.json');
        expect(result).toEqual({ id: 'api-1', name: 'My API' });
    });

    it('updateApiFromSwagger PUTs the descriptor as JSON to _import/swagger', async () => {
        const tracker = trackHandler('put', SWAGGER_PATH, { id: 'api-1', name: 'My API' });
        const descriptor = { payload: 'openapi: 3.0.0', withDocumentation: true, withOASValidationPolicy: false };

        const result = await updateApiFromSwagger('DEFAULT', 'api-1', descriptor);

        expect(tracker.callCount).toBe(1);
        expect(new URL(tracker.lastCall!.url).pathname).toContain('/apis/api-1/_import/swagger');
        expect(tracker.lastCall?.method).toBe('PUT');
        expect(tracker.lastCall?.headers.get('content-type')).toContain('application/json');
        expect(tracker.lastCall?.body).toEqual(descriptor);
        expect(result).toEqual({ id: 'api-1', name: 'My API' });
    });
});
