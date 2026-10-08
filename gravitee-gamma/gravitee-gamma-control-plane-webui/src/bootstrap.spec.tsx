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
import { screen, waitFor } from '@testing-library/react';

jest.mock('./monaco-setup', () => ({}));
jest.mock('./bootstrap-initialize', () => ({
    runApplicationBootstrap: () => Promise.reject(new Error('Failed to fetch bootstrap config: 401')),
}));

describe('bootstrap', () => {
    it('should show the startup error page instead of a blank page when the console cannot start', async () => {
        jest.spyOn(console, 'error').mockImplementation(() => undefined);
        document.body.innerHTML = '<div id="root"></div>';

        await jest.isolateModulesAsync(async () => {
            await import('./bootstrap');
        });

        await waitFor(() => expect(screen.getByRole('alert').textContent).toContain("The console couldn't start"));
    });
});
