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
import { integrationMembersLoadErrorMessage } from './integrationMembersLoadErrorMessage';
import { ApimApiError } from '../../../shared/api/apimClient';

describe('integrationMembersLoadErrorMessage', () => {
    it.each([
        {
            case: 'returns the body message when it is a non-empty string',
            error: new ApimApiError(500, 'x', { message: 'Members could not be loaded' }),
            expectedMessage: 'Members could not be loaded',
        },
        {
            case: 'falls back when the body message is an empty string',
            error: new ApimApiError(500, 'x', { message: '' }),
            expectedMessage: 'Failed to load members.',
        },
        {
            case: 'falls back when the body message is not a string',
            error: new ApimApiError(500, 'x', { message: 42 }),
            expectedMessage: 'Failed to load members.',
        },
        {
            case: 'falls back when the body is null',
            error: new ApimApiError(500, 'x', null),
            expectedMessage: 'Failed to load members.',
        },
    ])('$case', ({ error, expectedMessage }) => {
        expect(integrationMembersLoadErrorMessage(error)).toBe(expectedMessage);
    });
});
