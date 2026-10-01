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
import { validateIntegrationForm } from './integrationForm';

describe('validateIntegrationForm', () => {
    it('requires a name', () => {
        expect(validateIntegrationForm({ name: '', description: '' })).toEqual({ name: 'Name is required.' });
    });

    it.each([
        {
            case: 'accepts a name at the maximum length and an empty description',
            name: 'n'.repeat(50),
            description: '',
            expectedErrors: {},
        },
        {
            case: 'rejects a name one character over the maximum',
            name: 'n'.repeat(51),
            description: '',
            expectedErrors: { name: 'Name can not exceed 50 characters.' },
        },
        { case: 'accepts a description at the maximum length', name: 'My integration', description: 'd'.repeat(250), expectedErrors: {} },
        {
            case: 'rejects a description one character over the maximum',
            name: 'My integration',
            description: 'd'.repeat(251),
            expectedErrors: { description: 'Description can not exceed 250 characters.' },
        },
        { case: 'accepts a whitespace-only name without trimming it', name: ' ', description: '', expectedErrors: {} },
        {
            case: 'counts trailing whitespace toward the name length',
            name: `${'n'.repeat(50)} `,
            description: '',
            expectedErrors: { name: 'Name can not exceed 50 characters.' },
        },
    ])('$case', ({ name, description, expectedErrors }) => {
        expect(validateIntegrationForm({ name, description })).toEqual(expectedErrors);
    });

    it('reports name and description errors together', () => {
        expect(validateIntegrationForm({ name: '', description: 'd'.repeat(251) })).toEqual({
            name: 'Name is required.',
            description: 'Description can not exceed 250 characters.',
        });
    });
});
