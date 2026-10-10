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
import { ingestionErrorMessage } from './ingestionErrorMessage';
import { apimFetchJsonV2, resetApimClientForTests } from '../../../shared/api/apimClient';

const INGEST_PATH = '/integrations/integration-1/_ingest';
const INGEST_URL = `https://apim.test/management/v2/environments/env-1${INGEST_PATH}`;
const INGESTION_FAILED = 'Ingestion failed. Please check your settings and try again:';

function jsonResponse(body: unknown) {
    return Promise.resolve(new Response(JSON.stringify(body), { status: 200, headers: { 'Content-Type': 'application/json' } }));
}

// The API client resolves /constants.json then /ui/bootstrap before any environment-scoped call, so both answer
// here for the _ingest request to reach the network stub.
function stubIngestFetch(ingestResponse: () => Promise<Response>) {
    resetApimClientForTests();
    return jest.spyOn(global, 'fetch').mockImplementation(input => {
        const url = String(input);
        if (url.endsWith('/constants.json')) return jsonResponse({ gammaBaseURL: 'https://apim.test/gamma' });
        if (url.endsWith('/ui/bootstrap')) {
            return jsonResponse({
                managementBaseURL: 'https://apim.test/management',
                gammaBaseURL: 'https://apim.test/gamma',
                organizationId: 'org-1',
            });
        }
        if (url === INGEST_URL) return ingestResponse();
        throw new Error(`Unexpected request to ${url}`);
    });
}

function sendIngestRequest(): Promise<unknown> {
    return apimFetchJsonV2('env-1', INGEST_PATH, { method: 'POST', body: JSON.stringify({ apiIds: ['api-n'] }) }).catch(
        (error: unknown) => error,
    );
}

describe('ingestionErrorMessage', () => {
    let fetchSpy: jest.SpyInstance | undefined;

    afterEach(() => {
        fetchSpy?.mockRestore();
        fetchSpy = undefined;
    });

    it.each([
        ['a 500 with an empty body', () => Promise.resolve(new Response('', { status: 500 })), `${INGESTION_FAILED} Something went wrong.`],
        ['a network error with no response', () => Promise.reject(new TypeError('Failed to fetch')), `${INGESTION_FAILED} Failed to fetch`],
    ])('describes an _ingest request that failed with %s', async (_failure, ingestResponse, expectedMessage) => {
        fetchSpy = stubIngestFetch(ingestResponse);
        const error = await sendIngestRequest();

        const message = ingestionErrorMessage(error);

        expect(message).toBe(expectedMessage);
        expect(message).not.toContain('undefined');
    });
});
