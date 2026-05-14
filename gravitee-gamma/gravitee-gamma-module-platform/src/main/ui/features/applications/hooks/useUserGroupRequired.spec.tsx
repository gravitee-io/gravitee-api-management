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
import { renderHook, waitFor } from '@testing-library/react';
import type { ReactNode } from 'react';

import { useUserGroupRequired } from './useUserGroupRequired';
import { ConsoleSettingsProvider } from '../../../shared/console-settings';
import { fetchOrgConsoleSettings } from '../../../shared/services/orgConsoleSettings';

jest.mock('../../../shared/services/orgConsoleSettings');

const mockFetchOrgConsoleSettings = jest.mocked(fetchOrgConsoleSettings);

function wrapper({ children }: { children: ReactNode }) {
    return <ConsoleSettingsProvider>{children}</ConsoleSettingsProvider>;
}

describe('useUserGroupRequired', () => {
    beforeEach(() => {
        mockFetchOrgConsoleSettings.mockReset();
    });

    it('returns false when user groups are not required', async () => {
        mockFetchOrgConsoleSettings.mockResolvedValue({});

        const { result } = renderHook(() => useUserGroupRequired(), { wrapper });

        await waitFor(() => {
            expect(result.current.requireUserGroups).toBe(false);
        });
    });

    it('reads requireUserGroups from loaded settings', async () => {
        mockFetchOrgConsoleSettings.mockResolvedValue({ userGroup: { required: { enabled: true } } });

        const { result } = renderHook(() => useUserGroupRequired(), { wrapper });

        await waitFor(() => {
            expect(result.current.requireUserGroups).toBe(true);
        });
    });
});
