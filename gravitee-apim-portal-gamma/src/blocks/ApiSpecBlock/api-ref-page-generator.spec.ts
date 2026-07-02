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
import { buildTagPageDefinitions, createTagReferenceDocument } from './api-ref-page-generator';

describe('api-ref-page-generator', () => {
    it('creates a tag page with all API reference blocks', () => {
        const document = createTagReferenceDocument('Products');

        expect(document).toHaveLength(6);
        expect(document[2]).toMatchObject({
            type: 'graviteeApiOperations',
            props: { tag: 'Products', showResponses: 'true' },
        });
        expect(document[3]).toMatchObject({ type: 'graviteeApiSchemas', props: { tag: 'Products' } });
        expect(document[4]).toMatchObject({ type: 'graviteeApiTryIt', props: { tag: 'Products' } });
        expect(document[5]).toMatchObject({ type: 'graviteeApiCodeSamples', props: { tag: 'Products' } });
    });

    it('builds overview and tag pages from an API spec', async () => {
        const result = await buildTagPageDefinitions('api-payments', 'Payments API');

        expect(result.overviewDocument[0]).toMatchObject({ type: 'heading' });
        expect(result.tagPages.length).toBeGreaterThan(0);
        expect(result.tagPages[0].document[2]).toMatchObject({
            type: 'graviteeApiOperations',
        });
    });
});
