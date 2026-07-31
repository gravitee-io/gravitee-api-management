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
import {
    buildElementVarName,
    collapseCustomVarCssValue,
    expandColorValueIfCustomVarName,
    foundationTokenToCssVar,
    toKebab,
} from './var-names';

describe('var-names', () => {
    it('should convert camelCase to kebab-case', () => {
        expect(toKebab('primaryForeground')).toBe('primary-foreground');
    });

    it('should build foundation css var names', () => {
        expect(foundationTokenToCssVar('primary')).toBe('--portal-color-primary');
        expect(foundationTokenToCssVar('fontFamily')).toBe('--portal-font-font-family');
        expect(foundationTokenToCssVar('headerHeight')).toBe('--portal-layout-header-height');
    });

    it('should build element css var names', () => {
        expect(buildElementVarName('button', 'filled', 'borderRadius')).toBe('--portal-button-filled-border-radius');
        expect(buildElementVarName('header', undefined, 'background')).toBe('--portal-header-background');
        expect(buildElementVarName('header', undefined, 'logoSize')).toBe('--portal-header-logo-size');
        expect(buildElementVarName('nav-item', 'selected', 'background')).toBe('--portal-nav-item-selected-background');
    });

    describe('expandColorValueIfCustomVarName', () => {
        it('should expand a bare custom-var name', () => {
            expect(expandColorValueIfCustomVarName('my-var')).toBe('var(--portal-custom-my-var)');
        });

        it('should leave hex, transparent, and existing var() values unchanged', () => {
            expect(expandColorValueIfCustomVarName('#fff')).toBe('#fff');
            expect(expandColorValueIfCustomVarName('transparent')).toBe('transparent');
            expect(expandColorValueIfCustomVarName('var(--portal-custom-brand)')).toBe('var(--portal-custom-brand)');
            expect(expandColorValueIfCustomVarName('var(--portal-color-primary)')).toBe('var(--portal-color-primary)');
        });

        it('should leave empty values unchanged', () => {
            expect(expandColorValueIfCustomVarName('')).toBe('');
            expect(expandColorValueIfCustomVarName('   ')).toBe('');
        });
    });

    describe('collapseCustomVarCssValue', () => {
        it('should collapse a custom-var CSS value to the bare name', () => {
            expect(collapseCustomVarCssValue('var(--portal-custom-my-var)')).toBe('my-var');
            expect(collapseCustomVarCssValue('var( --portal-custom-brand )')).toBe('brand');
        });

        it('should leave non-custom-var values unchanged', () => {
            expect(collapseCustomVarCssValue('#6366f1')).toBe('#6366f1');
            expect(collapseCustomVarCssValue('transparent')).toBe('transparent');
            expect(collapseCustomVarCssValue('var(--portal-color-primary)')).toBe('var(--portal-color-primary)');
        });
    });
});
