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
import { extractErrorMessage } from './extractErrorMessage';
import { ApimApiError } from '../api/apimClient';

describe('extractErrorMessage', () => {
    it('returns ApimApiError message', () => {
        expect(extractErrorMessage(new ApimApiError(400, 'Invalid image format : Image mime-type image/webp is not allowed'))).toBe(
            'Invalid image format : Image mime-type image/webp is not allowed',
        );
    });

    it('returns Error message', () => {
        expect(extractErrorMessage(new Error('Failed to save changes.'))).toBe('Failed to save changes.');
    });

    it('returns string errors', () => {
        expect(extractErrorMessage('Bad request')).toBe('Bad request');
    });

    it('returns fallback for unknown values', () => {
        expect(extractErrorMessage(null, 'Fallback')).toBe('Fallback');
    });
});
