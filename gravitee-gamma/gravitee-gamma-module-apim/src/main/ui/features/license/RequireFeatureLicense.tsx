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
import { useModuleRouting } from '@gravitee/gamma-modules-sdk/routing';
import { useState, type ReactNode } from 'react';

import { APIM_FEATURE_UPGRADES, type ApimLicenseFeatureId } from './apimFeatures';
import { APIM_ROUTE_CONFIG, type RouteKey } from '../../config/routes';
import { FeatureUpgradeDialog } from '../../shared/components/FeatureUpgradeDialog';

/**
 * Generic route gate for any APIM license feature registered in {@link APIM_FEATURE_UPGRADES}.
 * When the feature is missing from the org license, blocks children and shows the upgrade dialog.
 * Closing the dialog navigates to `fallbackRouteKey` (defaults to Quick Start).
 *
 * Keep the dialog open until navigation unmounts this gate — clearing `open` first would flash
 * an empty outlet on the gated route.
 */
export function RequireFeatureLicense({
    feature,
    fallbackRouteKey = 'quick-start',
    children,
}: {
    readonly feature: ApimLicenseFeatureId;
    readonly fallbackRouteKey?: RouteKey;
    readonly children: ReactNode;
}) {
    const hasFeature = useHasFeature(feature);
    const { navigateToKey } = useModuleRouting(APIM_ROUTE_CONFIG);
    const [upgradeOpen, setUpgradeOpen] = useState(true);

    if (hasFeature) {
        return children;
    }

    function handleUpgradeOpenChange(open: boolean) {
        if (!open) {
            navigateToKey(fallbackRouteKey);
            return;
        }
        setUpgradeOpen(open);
    }

    return <FeatureUpgradeDialog content={APIM_FEATURE_UPGRADES[feature]} open={upgradeOpen} onOpenChange={handleUpgradeOpenChange} />;
}
