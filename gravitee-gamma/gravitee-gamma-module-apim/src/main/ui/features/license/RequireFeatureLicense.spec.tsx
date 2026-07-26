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
import { useHasFeature } from '@gravitee/gamma-modules-sdk';
import { render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';

import { APIM_FEATURE_UPGRADES, ApimLicenseFeature } from './apimFeatures';
import { RequireFeatureLicense } from './RequireFeatureLicense';

const mockNavigateToKey = jest.fn();

jest.mock('@gravitee/gamma-modules-sdk', () => ({
    useHasFeature: jest.fn(),
}));

jest.mock('@gravitee/gamma-modules-sdk/routing', () => ({
    useModuleRouting: () => ({ navigateToKey: mockNavigateToKey, modulePrefix: 'apim' }),
}));

const mockUseHasFeature = useHasFeature as jest.MockedFunction<typeof useHasFeature>;
const FEATURE = ApimLicenseFeature.API_PRODUCTS;
const CONTENT = APIM_FEATURE_UPGRADES[FEATURE];

describe('RequireFeatureLicense', () => {
    beforeEach(() => {
        mockNavigateToKey.mockClear();
    });

    it('should render children when the feature is licensed', () => {
        mockUseHasFeature.mockReturnValue(true);

        render(
            <RequireFeatureLicense feature={FEATURE}>
                <div>Licensed content</div>
            </RequireFeatureLicense>,
        );

        expect(screen.getByText('Licensed content')).toBeTruthy();
        expect(screen.queryByRole('heading', { name: CONTENT.title })).toBeNull();
    });

    it('should show the upgrade dialog and block children when unlicensed', () => {
        mockUseHasFeature.mockReturnValue(false);

        render(
            <RequireFeatureLicense feature={FEATURE}>
                <div>Licensed content</div>
            </RequireFeatureLicense>,
        );

        expect(screen.queryByText('Licensed content')).toBeNull();
        expect(screen.getByRole('heading', { name: CONTENT.title })).toBeTruthy();
    });

    it('should navigate to quick-start by default when the upgrade dialog is closed', async () => {
        mockUseHasFeature.mockReturnValue(false);
        const user = userEvent.setup();

        render(
            <RequireFeatureLicense feature={FEATURE}>
                <div>Licensed content</div>
            </RequireFeatureLicense>,
        );

        await user.keyboard('{Escape}');

        expect(mockNavigateToKey).toHaveBeenCalledWith('quick-start');
        // Dialog stays open until navigation unmounts the gate (avoids empty outlet flash).
        expect(screen.getByRole('heading', { name: CONTENT.title })).toBeTruthy();
    });

    it('should navigate to a custom fallback route when provided', async () => {
        mockUseHasFeature.mockReturnValue(false);
        const user = userEvent.setup();

        render(
            <RequireFeatureLicense feature={FEATURE} fallbackRouteKey="apis">
                <div>Licensed content</div>
            </RequireFeatureLicense>,
        );

        await user.keyboard('{Escape}');

        expect(mockNavigateToKey).toHaveBeenCalledWith('apis');
    });
});
