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

import { useEnvironment } from '@gravitee/gamma-modules-sdk';
import { act, renderHook } from '@testing-library/react';

import { useBoundPortalSettingsForm } from './useBoundPortalSettingsForm';
import type { PortalSettings } from '../../security-plan-types/services/portalSettings';

jest.mock('@gravitee/gamma-modules-sdk', () => ({
    useEnvironment: jest.fn(),
}));

const mockUseEnvironment = jest.mocked(useEnvironment);

function buildHost(settings: PortalSettings | undefined): string {
    return settings?.email?.host ?? '';
}

describe('useBoundPortalSettingsForm', () => {
    beforeEach(() => {
        mockUseEnvironment.mockReturnValue({ id: 'env-1' } as ReturnType<typeof useEnvironment>);
    });

    it('does not treat late env.id hydration as an environment switch that wipes edits', () => {
        mockUseEnvironment.mockReturnValue({} as ReturnType<typeof useEnvironment>);
        const { result, rerender } = renderHook(
            ({ settings }: { settings: PortalSettings | undefined }) => useBoundPortalSettingsForm(settings, buildHost),
            { initialProps: { settings: { email: { host: 'smtp.early.com' } } } },
        );

        act(() => {
            result.current.setLocalState('smtp.draft.com');
        });

        mockUseEnvironment.mockReturnValue({ id: 'env-1' } as ReturnType<typeof useEnvironment>);
        rerender({ settings: { email: { host: 'smtp.early.com' } } });
        expect(result.current.localState).toBe('smtp.draft.com');
    });

    it('does not clobber in-progress edits on same-env refetch', () => {
        const { result, rerender } = renderHook(
            ({ settings }: { settings: PortalSettings | undefined }) => useBoundPortalSettingsForm(settings, buildHost),
            { initialProps: { settings: { email: { host: 'smtp.example.com' } } } },
        );

        act(() => {
            result.current.setLocalState('smtp.draft.com');
        });
        rerender({ settings: { email: { host: 'smtp.example.com' } } });
        expect(result.current.localState).toBe('smtp.draft.com');
    });

    it('skips onCleanHydrate while the form is dirty', () => {
        const onCleanHydrate = jest.fn();
        const { result, rerender } = renderHook(
            ({ settings }: { settings: PortalSettings | undefined }) => useBoundPortalSettingsForm(settings, buildHost, onCleanHydrate),
            { initialProps: { settings: { email: { host: 'smtp.example.com', brandedSendersInherited: false } } } },
        );

        expect(onCleanHydrate).toHaveBeenCalledTimes(1);
        act(() => {
            result.current.setLocalState('smtp.draft.com');
        });
        rerender({ settings: { email: { host: 'smtp.example.com', brandedSendersInherited: true } } });
        expect(onCleanHydrate).toHaveBeenCalledTimes(1);
        expect(result.current.localState).toBe('smtp.draft.com');
    });
});
