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
import { colorIsDark, getProviderTextColor } from './idp.utils';

describe('idp.utils', () => {
    describe('colorIsDark', () => {
        it('should return true for black', () => {
            expect(colorIsDark('#000000')).toBe(true);
        });

        it('should return false for white', () => {
            expect(colorIsDark('#FFFFFF')).toBe(false);
        });

        it('should return true for dark colors', () => {
            expect(colorIsDark('#333333')).toBe(true);
            expect(colorIsDark('#444444')).toBe(true);
        });

        it('should return false for light colors', () => {
            expect(colorIsDark('#EEEEEE')).toBe(false);
            expect(colorIsDark('#86c3d0')).toBe(false);
        });
    });

    describe('getProviderTextColor', () => {
        it('should return white when no color provided', () => {
            expect(getProviderTextColor()).toBe('white');
            expect(getProviderTextColor(undefined)).toBe('white');
        });

        it('should return white for dark backgrounds', () => {
            expect(getProviderTextColor('#000000')).toBe('white');
            expect(getProviderTextColor('#333333')).toBe('white');
        });

        it('should return black for light backgrounds', () => {
            expect(getProviderTextColor('#FFFFFF')).toBe('black');
            expect(getProviderTextColor('#86c3d0')).toBe('black');
        });
    });
});
