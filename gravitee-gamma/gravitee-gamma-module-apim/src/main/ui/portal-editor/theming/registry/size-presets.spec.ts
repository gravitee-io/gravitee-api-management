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
import { resolveSizeValue } from './size-presets';

describe('size-presets', () => {
    it('should resolve logoSize presets', () => {
        expect(resolveSizeValue('logoSize', 'xs')).toBe('16px');
        expect(resolveSizeValue('logoSize', 'sm')).toBe('20px');
        expect(resolveSizeValue('logoSize', 'md')).toBe('24px');
        expect(resolveSizeValue('logoSize', 'lg')).toBe('32px');
        expect(resolveSizeValue('logoSize', 'xl')).toBe('40px');
    });

    it('should pass through custom logoSize values', () => {
        expect(resolveSizeValue('logoSize', '28px')).toBe('28px');
    });
});
