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
import { render, screen, waitFor } from '@testing-library/react';

import { fetchOrgConsoleSettings } from '../services/orgConsoleSettings';

import { ConsoleSettingsProvider, useConsoleSettings, useConsoleSettingsReady } from './index';

jest.mock('../services/orgConsoleSettings');

const mockFetchOrgConsoleSettings = jest.mocked(fetchOrgConsoleSettings);

function SettingsProbe() {
    const settings = useConsoleSettings();
    const isReady = useConsoleSettingsReady();
    return (
        <div>
            <span data-testid="ready">{String(isReady)}</span>
            <span data-testid="require-groups">{String(settings?.userGroup?.required?.enabled ?? false)}</span>
        </div>
    );
}

describe('ConsoleSettingsProvider', () => {
    beforeEach(() => {
        mockFetchOrgConsoleSettings.mockReset();
    });

    it('shows loading UI while console settings are fetched', () => {
        mockFetchOrgConsoleSettings.mockReturnValue(new Promise(() => {}));

        render(
            <ConsoleSettingsProvider>
                <SettingsProbe />
            </ConsoleSettingsProvider>,
        );

        expect(screen.getByText('Loading organization settings…')).toBeTruthy();
    });

    it('loads settings and exposes them to children', async () => {
        mockFetchOrgConsoleSettings.mockResolvedValue({ userGroup: { required: { enabled: true } } });

        render(
            <ConsoleSettingsProvider>
                <SettingsProbe />
            </ConsoleSettingsProvider>,
        );

        await waitFor(() => {
            expect(screen.getByTestId('ready').textContent).toBe('true');
        });
        expect(screen.getByTestId('require-groups').textContent).toBe('true');
    });

    it('shows an error when the console settings fetch fails', async () => {
        mockFetchOrgConsoleSettings.mockRejectedValue(new Error('Failed to fetch console config: 503'));

        render(
            <ConsoleSettingsProvider>
                <SettingsProbe />
            </ConsoleSettingsProvider>,
        );

        await waitFor(() => {
            expect(screen.getByText('Management API unreachable or error occurs, please check logs')).toBeTruthy();
        });
    });
});
