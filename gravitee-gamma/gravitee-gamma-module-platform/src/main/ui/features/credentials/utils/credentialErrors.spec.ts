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
import { credentialFailureMessage } from './credentialErrors';
import { ApimApiError } from '../../../shared/api/apimClient';

describe('credentialFailureMessage', () => {
    it('shows the reason the server gives for a refused request', () => {
        expect(credentialFailureMessage(new ApimApiError(400, 'Name is already used.'), 'Could not save.')).toBe('Name is already used.');
    });

    it.each([500, 503])('hides the server detail of a %s behind the fallback', status => {
        const error = new ApimApiError(status, 'Secret values cannot be stored or read: api.secrets.encryption.key is not set.');

        expect(credentialFailureMessage(error, 'Could not save.')).toBe('Could not save.');
    });

    it('falls back when the failure has no message', () => {
        expect(credentialFailureMessage(undefined, 'Could not save.')).toBe('Could not save.');
    });
});
