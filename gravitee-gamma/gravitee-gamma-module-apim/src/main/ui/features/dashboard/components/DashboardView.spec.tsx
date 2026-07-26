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
import { render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';

import { DashboardView } from './DashboardView';
import { APIM_FEATURE_UPGRADES, ApimLicenseFeature } from '../../license/apimFeatures';

const CONTENT = APIM_FEATURE_UPGRADES[ApimLicenseFeature.API_PRODUCTS];

const baseProps = {
    totalApis: 3,
    totalProducts: 0,
    onCreateProxy: jest.fn(),
    onCreateProduct: jest.fn(),
    onGoToApis: jest.fn(),
    onGoToApiProducts: jest.fn(),
};

describe('DashboardView', () => {
    beforeEach(() => {
        jest.clearAllMocks();
    });

    it('should show Upgrade to access CTAs and open the upgrade dialog when API Products are locked', async () => {
        const user = userEvent.setup();

        render(<DashboardView {...baseProps} apiProductsLocked />);

        const upgradeButtons = screen.getAllByRole('button', { name: /upgrade to access/i });
        expect(upgradeButtons.length).toBeGreaterThanOrEqual(2);

        await user.click(upgradeButtons[0]);

        expect(screen.getByRole('heading', { name: CONTENT.title })).toBeTruthy();
        expect(baseProps.onGoToApiProducts).not.toHaveBeenCalled();
        expect(baseProps.onCreateProduct).not.toHaveBeenCalled();
    });

    it('should not show Upgrade to access CTAs when API Products are licensed', () => {
        render(<DashboardView {...baseProps} apiProductsLocked={false} />);

        expect(screen.queryByRole('button', { name: /upgrade to access/i })).toBeNull();
        expect(screen.getByRole('button', { name: /create api product/i })).toBeTruthy();
    });
});
