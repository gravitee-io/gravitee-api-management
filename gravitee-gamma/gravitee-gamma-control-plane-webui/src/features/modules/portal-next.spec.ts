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
import { buildPortalNextEditorUrl } from './portal-next';

describe('buildPortalNextEditorUrl', () => {
    it('should send the default environment to the portal-next editor', () => {
        expect(buildPortalNextEditorUrl('http://localhost:4000', 'default')).toBe('http://localhost:4000/#!/default/_portal/navigation');
    });

    it('should trim a trailing slash on the console origin', () => {
        expect(buildPortalNextEditorUrl('https://console.example.com/', 'env-1')).toBe(
            'https://console.example.com/#!/env-1/_portal/navigation',
        );
    });

    it('should encode the environment HRID as a path segment', () => {
        expect(buildPortalNextEditorUrl('http://localhost:4000', 'env/prod')).toBe(
            'http://localhost:4000/#!/env%2Fprod/_portal/navigation',
        );
    });
});
