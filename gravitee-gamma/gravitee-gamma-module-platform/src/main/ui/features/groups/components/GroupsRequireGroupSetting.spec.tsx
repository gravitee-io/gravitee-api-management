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
import { useHasPermission } from '@gravitee/gamma-modules-sdk';
import { fireEvent, render, screen, waitFor } from '@testing-library/react';

import { GroupsRequireGroupSetting } from './GroupsRequireGroupSetting';
import { useConsoleSettings, useSetConsoleSettings, type ConsoleSettings } from '../../../shared/console-settings';
import { notify } from '../../../shared/notify';
import { saveOrgConsoleSettings } from '../../../shared/services/orgConsoleSettings';

jest.mock('../../../shared/console-settings', () => ({
    ...jest.requireActual('../../../shared/console-settings'),
    useConsoleSettings: jest.fn(),
    useSetConsoleSettings: jest.fn(),
}));
jest.mock('../../../shared/services/orgConsoleSettings');
jest.mock('../../../shared/notify', () => ({
    notify: { success: jest.fn(), error: jest.fn(), warning: jest.fn() },
}));
jest.mock('@gravitee/gamma-modules-sdk', () => ({
    useHasPermission: jest.fn(),
}));

const mockUseConsoleSettings = jest.mocked(useConsoleSettings);
const mockUseSetConsoleSettings = jest.mocked(useSetConsoleSettings);
const mockSaveOrgConsoleSettings = jest.mocked(saveOrgConsoleSettings);
const mockUseHasPermission = jest.mocked(useHasPermission);

describe('GroupsRequireGroupSetting', () => {
    const setConsoleSettings = jest.fn();

    beforeEach(() => {
        jest.clearAllMocks();
        mockUseSetConsoleSettings.mockReturnValue(setConsoleSettings);
        mockUseHasPermission.mockReturnValue(true);
    });

    it('reflects the currently-loaded setting and disables Save until changed', () => {
        mockUseConsoleSettings.mockReturnValue({ userGroup: { required: { enabled: true } } });
        render(<GroupsRequireGroupSetting />);

        expect(screen.getByRole('switch').getAttribute('aria-checked')).toBe('true');
        expect(screen.getByRole('button', { name: 'Save' })).toHaveProperty('disabled', true);
    });

    it('defaults to off when the setting is missing entirely', () => {
        mockUseConsoleSettings.mockReturnValue(null);
        render(<GroupsRequireGroupSetting />);
        expect(screen.getByRole('switch').getAttribute('aria-checked')).toBe('false');
    });

    it('keeps Save disabled without organization-settings-u even after flipping the toggle', () => {
        mockUseHasPermission.mockReturnValue(false);
        mockUseConsoleSettings.mockReturnValue({ userGroup: { required: { enabled: false } } });
        render(<GroupsRequireGroupSetting />);

        expect(screen.getByRole('switch')).toHaveProperty('disabled', true);
        expect(screen.getByRole('button', { name: 'Save' })).toHaveProperty('disabled', true);
    });

    it('enables Save once the toggle is flipped, and preserves other settings fields on save', async () => {
        mockUseConsoleSettings.mockReturnValue({
            userGroup: { required: { enabled: false } },
            authentication: { localLogin: { enabled: true } },
        } as ConsoleSettings & { authentication: { localLogin: { enabled: boolean } } });
        mockSaveOrgConsoleSettings.mockResolvedValue({ userGroup: { required: { enabled: true } } });
        render(<GroupsRequireGroupSetting />);

        fireEvent.click(screen.getByRole('switch'));
        expect(screen.getByRole('button', { name: 'Save' })).toHaveProperty('disabled', false);

        fireEvent.click(screen.getByRole('button', { name: 'Save' }));

        await waitFor(() =>
            expect(mockSaveOrgConsoleSettings).toHaveBeenCalledWith({
                userGroup: { required: { enabled: true } },
                authentication: { localLogin: { enabled: true } },
            }),
        );
        await waitFor(() => expect(notify.success).toHaveBeenCalledWith('Successfully updated groups settings.'));
        expect(setConsoleSettings).toHaveBeenCalledWith({ userGroup: { required: { enabled: true } } });
    });

    it('shows an error toast when saving fails', async () => {
        mockUseConsoleSettings.mockReturnValue({ userGroup: { required: { enabled: false } } });
        mockSaveOrgConsoleSettings.mockRejectedValue(new Error('failed'));
        render(<GroupsRequireGroupSetting />);

        fireEvent.click(screen.getByRole('switch'));
        fireEvent.click(screen.getByRole('button', { name: 'Save' }));

        await waitFor(() => expect(notify.error).toHaveBeenCalledWith(expect.any(Error), 'Error occurred while saving groups settings.'));
        expect(setConsoleSettings).not.toHaveBeenCalled();
    });
});
