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
import { MASKED_SECRET, maskedSecretKeys, sameSourceAddress } from './sourceSecrets';

describe('sameSourceAddress', () => {
    it('is the same address when the repository and every URL field are unchanged', () => {
        expect(
            sameSourceAddress(
                { githubUrl: 'https://api.github.com', owner: 'acme', repository: 'petstore', filepath: 'a.yaml' },
                { githubUrl: 'https://api.github.com', owner: 'other', repository: 'petstore', filepath: 'b.yaml' },
            ),
        ).toBe(true);
    });

    it('is another address when the repository changes', () => {
        expect(sameSourceAddress({ repository: 'petstore' }, { repository: 'billing' })).toBe(false);
    });

    it('is another address when a field ending in Url or url changes', () => {
        expect(sameSourceAddress({ gitlabUrl: 'https://gitlab.com' }, { gitlabUrl: 'https://gitlab.acme.io' })).toBe(false);
        expect(sameSourceAddress({ url: 'https://a.io/doc.md' }, { url: 'https://b.io/doc.md' })).toBe(false);
    });

    it('is another address when an address field is added or removed', () => {
        expect(sameSourceAddress({ owner: 'acme' }, { owner: 'acme', repository: 'petstore' })).toBe(false);
        expect(sameSourceAddress({ url: 'https://a.io' }, {})).toBe(false);
    });

    it('treats a missing field and a null one as the same', () => {
        expect(sameSourceAddress({ url: null }, {})).toBe(true);
    });
});

describe('maskedSecretKeys', () => {
    it('names the fields the server sent back masked', () => {
        expect(maskedSecretKeys({ repository: 'petstore', personalAccessToken: MASKED_SECRET, password: MASKED_SECRET })).toEqual([
            'personalAccessToken',
            'password',
        ]);
    });

    it('names none when no field is masked', () => {
        expect(maskedSecretKeys({ repository: 'petstore', personalAccessToken: 'ghp_x' })).toEqual([]);
    });
});
