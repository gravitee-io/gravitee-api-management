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
import { renderHook } from '@testing-library/react';

import { useApplicationNotificationPermissions } from './useApplicationNotificationPermissions';
import { useApplicationDetailContext } from '../context/ApplicationDetailContext';

jest.mock('@gravitee/gamma-modules-sdk', () => ({
    useHasPermission: jest.fn(),
}));

jest.mock('../context/ApplicationDetailContext', () => ({
    useApplicationDetailContext: jest.fn(),
}));

const mockUseHasPermission = jest.mocked(useHasPermission);
const mockUseApplicationDetailContext = jest.mocked(useApplicationDetailContext);

describe('useApplicationNotificationPermissions', () => {
    beforeEach(() => {
        jest.clearAllMocks();
        mockUseApplicationDetailContext.mockReturnValue({
            permissionsReady: true,
        } as ReturnType<typeof useApplicationDetailContext>);
        mockUseHasPermission.mockReturnValue(false);
    });

    it('gates notification and metadata flags until permissions are ready', () => {
        mockUseApplicationDetailContext.mockReturnValue({
            permissionsReady: false,
        } as ReturnType<typeof useApplicationDetailContext>);
        mockUseHasPermission.mockReturnValue(true);

        const { result } = renderHook(() => useApplicationNotificationPermissions());

        expect(result.current.canCreateNotification).toBe(false);
        expect(result.current.canDeleteMetadata).toBe(false);
    });

    it('maps notification and metadata permissions independently', () => {
        mockUseHasPermission.mockImplementation(({ anyOf }) => {
            if (anyOf?.includes('application-notification-d')) {
                return true;
            }
            if (anyOf?.includes('application-metadata-u')) {
                return true;
            }
            return false;
        });

        const { result } = renderHook(() => useApplicationNotificationPermissions());

        expect(result.current.canDeleteNotification).toBe(true);
        expect(result.current.canUpdateMetadata).toBe(true);
        expect(result.current.canCreateNotification).toBe(false);
    });
});
