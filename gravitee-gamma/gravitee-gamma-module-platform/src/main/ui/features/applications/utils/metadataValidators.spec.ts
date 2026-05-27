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
import { MAIL_PATTERN, URL_PATTERN } from './metadataValidators';

describe('metadataValidators', () => {
    describe('MAIL_PATTERN', () => {
        it('accepts valid emails and expression placeholders', () => {
            expect(MAIL_PATTERN.test('user@example.com')).toBe(true);
            expect(MAIL_PATTERN.test('${metadata.email}')).toBe(true);
        });

        it('rejects invalid emails', () => {
            expect(MAIL_PATTERN.test('not-an-email')).toBe(false);
            expect(MAIL_PATTERN.test('@missing-local.com')).toBe(false);
        });
    });

    describe('URL_PATTERN', () => {
        it('accepts URLs and expression placeholders', () => {
            expect(URL_PATTERN.test('https://api.example.com/path')).toBe(true);
            expect(URL_PATTERN.test('${metadata.callbackUrl}')).toBe(true);
        });

        it('rejects invalid URLs', () => {
            expect(URL_PATTERN.test('not a url')).toBe(false);
        });
    });
});
