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
import type { LucideIcon } from '@gravitee/graphene-core/icons';
import { ArchiveIcon } from '@gravitee/graphene-core/icons';

/**
 * License feature ids used by APIM Gamma UI.
 * Values must match classic console `ApimFeature` / backend license feature strings.
 */
export const ApimLicenseFeature = {
    API_PRODUCTS: 'apim-api-products',
} as const;

export type ApimLicenseFeatureId = (typeof ApimLicenseFeature)[keyof typeof ApimLicenseFeature];

/**
 * Destination of the "Request an enterprise license" CTA in upgrade dialogs.
 * Aligned with classic console (`gio-license-data`) and Gamma home `UpgradeDialog`.
 */
export const REQUEST_ENTERPRISE_LICENSE_URL = 'https://gravitee.io/self-hosted-trial';

export interface FeatureUpgradeContent {
    readonly featureId: ApimLicenseFeatureId;
    readonly title: string;
    readonly description: string;
    readonly Icon: LucideIcon;
    readonly features: readonly string[];
}

/** Upsell content for license-gated APIM features. */
export const APIM_FEATURE_UPGRADES: Record<ApimLicenseFeatureId, FeatureUpgradeContent> = {
    [ApimLicenseFeature.API_PRODUCTS]: {
        featureId: ApimLicenseFeature.API_PRODUCTS,
        title: 'API Products',
        description: 'Bundle multiple APIs into consumable products with unified plans, subscriptions, and access control.',
        Icon: ArchiveIcon,
        features: [
            'Bundle multiple APIs into one product surface',
            'Define plans and manage subscriptions in one place',
            'Give developers unified access through the portal',
        ],
    },
};
