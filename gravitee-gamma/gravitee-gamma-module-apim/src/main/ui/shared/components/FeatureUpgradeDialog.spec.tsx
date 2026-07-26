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

import { FeatureUpgradeDialog } from './FeatureUpgradeDialog';
import { APIM_FEATURE_UPGRADES, ApimLicenseFeature, REQUEST_ENTERPRISE_LICENSE_URL } from '../../features/license/apimFeatures';

const API_PRODUCTS = APIM_FEATURE_UPGRADES[ApimLicenseFeature.API_PRODUCTS];

describe('FeatureUpgradeDialog', () => {
    it('should render the feature title, description and every bullet when open', () => {
        render(<FeatureUpgradeDialog content={API_PRODUCTS} open onOpenChange={() => {}} />);

        expect(screen.getByRole('heading', { name: API_PRODUCTS.title })).toBeTruthy();
        expect(screen.getByText(API_PRODUCTS.description)).toBeTruthy();
        API_PRODUCTS.features.forEach(feature => {
            expect(screen.getByText(feature)).toBeTruthy();
        });
    });

    it('should point the enterprise license CTA to the configured URL', () => {
        render(<FeatureUpgradeDialog content={API_PRODUCTS} open onOpenChange={() => {}} />);

        const cta = screen.getByRole('link', { name: /request an enterprise license/i });
        expect(cta.getAttribute('href')).toBe(REQUEST_ENTERPRISE_LICENSE_URL);
    });

    it('should not render its content when closed', () => {
        render(<FeatureUpgradeDialog content={API_PRODUCTS} open={false} onOpenChange={() => {}} />);

        expect(screen.queryByText(API_PRODUCTS.features[0])).toBeNull();
    });
});
