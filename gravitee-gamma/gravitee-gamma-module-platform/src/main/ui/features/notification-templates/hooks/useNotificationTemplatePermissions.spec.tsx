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

import { useNotificationTemplatePermissions } from './useNotificationTemplatePermissions';

jest.mock('@gravitee/gamma-modules-sdk', () => ({
    useHasPermission: jest.fn(),
}));

const mockUseHasPermission = jest.mocked(useHasPermission);

describe('useNotificationTemplatePermissions', () => {
    it('maps Classic organization-notification_templates ACLs', () => {
        mockUseHasPermission.mockImplementation(({ anyOf }) => (anyOf ?? []).includes('organization-notification_templates-r'));

        const { result } = renderHook(() => useNotificationTemplatePermissions());

        expect(result.current.canRead).toBe(true);
        expect(result.current.canCreate).toBe(false);
        expect(result.current.canUpdate).toBe(false);
        expect(result.current.canEdit).toBe(false);
    });

    it('treats CREATE or UPDATE as enough to edit at least one channel', () => {
        mockUseHasPermission.mockImplementation(({ anyOf }) => (anyOf ?? []).includes('organization-notification_templates-c'));

        const { result } = renderHook(() => useNotificationTemplatePermissions());

        expect(result.current.canCreate).toBe(true);
        expect(result.current.canEdit).toBe(true);
    });
});
