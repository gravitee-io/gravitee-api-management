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
import { formatApplicationOwnerLabel, formatApplicationTypeLabel } from './applicationFormatters';
import type { ApplicationListItem } from '../types/application';

describe('applicationFormatters', () => {
    it('labels every application type with its console name', () => {
        expect(formatApplicationTypeLabel({ type: 'SIMPLE' })).toBe('Simple');
        expect(formatApplicationTypeLabel({ type: 'BROWSER' })).toBe('SPA');
        expect(formatApplicationTypeLabel({ type: 'WEB' })).toBe('Web');
        expect(formatApplicationTypeLabel({ type: 'NATIVE' })).toBe('Native');
        expect(formatApplicationTypeLabel({ type: 'BACKEND_TO_BACKEND' })).toBe('Backend to backend');
    });

    it('labels a simple application the same whether or not settings.app.type is set', () => {
        const withSettings = (appType: string): Pick<ApplicationListItem, 'type' | 'settings'> => ({
            type: 'SIMPLE',
            settings: { app: { type: appType } },
        });

        expect(formatApplicationTypeLabel({ type: 'SIMPLE' })).toBe('Simple');
        expect(formatApplicationTypeLabel(withSettings('simple'))).toBe('Simple');
        expect(formatApplicationTypeLabel(withSettings('iOS'))).toBe('Simple');
    });

    it('shows a dash when the application has no type', () => {
        expect(formatApplicationTypeLabel({})).toBe('—');
    });

    it('formats owner with role label', () => {
        expect(formatApplicationOwnerLabel({ id: '1', displayName: 'Alice Chen', type: 'USER' })).toBe('Alice Chen (User)');
        expect(formatApplicationOwnerLabel(undefined)).toBeNull();
    });
});
