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

import { useEnvironmentNotificationPermissions } from './useEnvironmentNotificationPermissions';

jest.mock('@gravitee/gamma-modules-sdk', () => ({
    useHasPermission: jest.fn(),
}));

const mockUseHasPermission = jest.mocked(useHasPermission);

describe('useEnvironmentNotificationPermissions', () => {
    beforeEach(() => {
        jest.clearAllMocks();
        mockUseHasPermission.mockReturnValue(false);
    });

    it('maps create/update/delete permissions independently', () => {
        mockUseHasPermission.mockImplementation(({ anyOf }) => Boolean(anyOf?.includes('environment-notification-c')));

        const { result } = renderHook(() => useEnvironmentNotificationPermissions());

        expect(result.current.canCreate).toBe(true);
        expect(result.current.canUpdateGeneric).toBe(false);
        expect(result.current.canUpdatePortal).toBe(false);
        expect(result.current.canDelete).toBe(false);
    });

    // Classic parity: NotificationConfigsResource#updatePortalNotificationSettings only requires READ.
    it('grants canUpdatePortal from read permission alone, independently of update permission', () => {
        mockUseHasPermission.mockImplementation(({ anyOf }) => Boolean(anyOf?.includes('environment-notification-r')));

        const { result } = renderHook(() => useEnvironmentNotificationPermissions());

        expect(result.current.canUpdatePortal).toBe(true);
        expect(result.current.canUpdateGeneric).toBe(false);
    });

    it('grants canUpdateGeneric from update permission', () => {
        mockUseHasPermission.mockImplementation(({ anyOf }) => Boolean(anyOf?.includes('environment-notification-u')));

        const { result } = renderHook(() => useEnvironmentNotificationPermissions());

        expect(result.current.canUpdateGeneric).toBe(true);
        expect(result.current.canUpdatePortal).toBe(false);
    });

    it('maps delete permission', () => {
        mockUseHasPermission.mockImplementation(({ anyOf }) => Boolean(anyOf?.includes('environment-notification-d')));

        const { result } = renderHook(() => useEnvironmentNotificationPermissions());

        expect(result.current.canDelete).toBe(true);
    });
});
