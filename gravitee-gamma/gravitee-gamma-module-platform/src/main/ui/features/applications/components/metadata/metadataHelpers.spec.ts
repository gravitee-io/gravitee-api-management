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
import { deriveMetadataKey, metadataFormat, metadataValue } from './metadataHelpers';
import type { ApplicationMetadata } from '../../types/applicationNotification';

describe('metadataHelpers', () => {
    describe('deriveMetadataKey', () => {
        it('slugifies display names', () => {
            expect(deriveMetadataKey('  My Custom Key  ')).toBe('my-custom-key');
            expect(deriveMetadataKey('API_URL')).toBe('api-url');
        });
    });

    describe('metadataFormat', () => {
        it('defaults to STRING when metadata is null', () => {
            expect(metadataFormat(null)).toBe('STRING');
        });

        it('returns metadata format when set', () => {
            expect(metadataFormat({ format: 'MAIL' } as ApplicationMetadata)).toBe('MAIL');
        });
    });

    describe('metadataValue', () => {
        it('returns empty string for null metadata', () => {
            expect(metadataValue(null)).toBe('');
        });

        it('prefers value over defaultValue', () => {
            expect(metadataValue({ value: 'live', defaultValue: 'default' } as ApplicationMetadata)).toBe('live');
        });

        it('truncates DATE values to yyyy-mm-dd', () => {
            expect(metadataValue({ format: 'DATE', value: '2024-06-15T10:00:00Z' } as ApplicationMetadata)).toBe('2024-06-15');
        });
    });
});
