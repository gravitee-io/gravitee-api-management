/*
 * Copyright (C) 2026 The Gravitee team (http://gravitee.io)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
import { ToggleGroup, ToggleGroupItem, toast } from '@gravitee/graphene-core';
import type { CSSProperties } from 'react';
import { useCallback, useMemo, useState } from 'react';

import { useEnvironmentStore } from '../../../features/environment/environment.store';
import { getPrimaryHrid } from '../../../features/environment/environment.utils';
import { addCloudEnvironment, useCloudEnvironmentStore } from '../cloud-environment.store';
import type { CloudProduct } from '../cloud.config';
import { CLOUD_ACCOUNT_PROFILE, CLOUD_GATEWAYS_MOCK } from '../cloud.config';
import { CloudEnvironmentsCard, type CloudEnvironmentItem, type ProductFilter } from '../components/CloudEnvironmentsCard';
import { CloudGatewaysCard } from '../components/CloudGatewaysCard';
import { CloudInfoBanner } from '../components/CloudInfoBanner';
import { CloudLearnAboutSection } from '../components/CloudLearnAboutSection';
import { CloudTrialCountdown } from '../components/CloudTrialCountdown';
import type { NewEnvironmentPayload } from '../components/NewEnvironmentDialog';

const CHIP_BASE = 'h-8 rounded-md border px-3 text-xs font-medium transition-colors';
const CHIP_INACTIVE = `${CHIP_BASE} border-border text-muted-foreground hover:bg-muted hover:text-foreground`;

const ACTIVE_CHIP_STYLE: CSSProperties = {
    borderColor: 'var(--primary)',
    color: 'var(--primary)',
    backgroundColor: 'color-mix(in oklab, var(--primary) 10%, transparent)',
};

function matchesProductFilter(product: CloudProduct, filter: ProductFilter): boolean {
    switch (filter) {
        case 'all-envs':
            return true;
        case 'apim-envs':
            return product === 'APIM';
        case 'am-envs':
            return product === 'AM';
    }
}

export function CloudOverviewPage() {
    const storeEnvironments = useEnvironmentStore(s => s.environments);
    const productByEnvironmentId = useCloudEnvironmentStore(s => s.productByEnvironmentId);
    const [productFilter, setProductFilter] = useState<ProductFilter>('all-envs');

    const environments = useMemo(() => {
        const defaultEnvironmentId = storeEnvironments[0]?.id;
        const cloudEnvironmentIds = new Set(Object.keys(productByEnvironmentId));

        return storeEnvironments
            .filter(
                environment =>
                    cloudEnvironmentIds.has(environment.id) || environment.id === defaultEnvironmentId,
            )
            .map(environment => {
                const hrid = getPrimaryHrid(environment);
                return {
                    id: environment.id,
                    name: environment.name ?? hrid,
                    hrid,
                    product: productByEnvironmentId[environment.id] ?? 'APIM',
                } satisfies CloudEnvironmentItem;
            });
    }, [productByEnvironmentId, storeEnvironments]);

    const filteredEnvironments = useMemo(
        () => environments.filter(environment => matchesProductFilter(environment.product, productFilter)),
        [environments, productFilter],
    );

    const { customer, trialDaysRemaining, canCreateEnvironment, canDeployGateway } = CLOUD_ACCOUNT_PROFILE;
    const envQuota = customer ? { current: environments.length, max: 10 } : undefined;

    const handleEnvironmentCreated = useCallback((payload: NewEnvironmentPayload) => {
        const created = addCloudEnvironment(payload);
        if (!created) {
            toast.error('Environment could not be created. The HRID may already be in use.');
            return;
        }
        toast.success('Environment has been created');
    }, []);

    const handleDeployGateway = useCallback(() => {
        toast.info('Gateway deployment will be available when the Cloud API is connected.');
    }, []);

    return (
        <div className="max-w-screen-xl space-y-6">
            <CloudInfoBanner />

            <div className="flex flex-col gap-4 sm:flex-row sm:items-start sm:justify-between">
                <div className="space-y-1">
                    <h1 className="text-2xl font-bold tracking-tight">Overview</h1>
                    <p className="text-sm text-muted-foreground">Get an overview of your Gravitee Cloud account.</p>
                </div>
                <ToggleGroup
                    type="single"
                    value={productFilter}
                    onValueChange={value => value && setProductFilter(value as ProductFilter)}
                    spacing={2}
                    aria-label="Filter by product"
                    className="page__components__header__filter"
                >
                    <ToggleGroupItem
                        value="all-envs"
                        className={productFilter === 'all-envs' ? CHIP_BASE : CHIP_INACTIVE}
                        style={productFilter === 'all-envs' ? ACTIVE_CHIP_STYLE : undefined}
                    >
                        All Products
                    </ToggleGroupItem>
                    <ToggleGroupItem
                        value="apim-envs"
                        className={productFilter === 'apim-envs' ? CHIP_BASE : CHIP_INACTIVE}
                        style={productFilter === 'apim-envs' ? ACTIVE_CHIP_STYLE : undefined}
                    >
                        APIM
                    </ToggleGroupItem>
                    <ToggleGroupItem
                        value="am-envs"
                        className={productFilter === 'am-envs' ? CHIP_BASE : CHIP_INACTIVE}
                        style={productFilter === 'am-envs' ? ACTIVE_CHIP_STYLE : undefined}
                    >
                        AM
                    </ToggleGroupItem>
                </ToggleGroup>
            </div>

            <CloudEnvironmentsCard
                environments={filteredEnvironments}
                isCustomer={customer}
                canCreateEnvironment={canCreateEnvironment}
                envQuota={envQuota}
                onEnvironmentCreated={handleEnvironmentCreated}
            />

            <CloudGatewaysCard
                gateways={CLOUD_GATEWAYS_MOCK}
                canDeployGateway={canDeployGateway}
                hasEnvironments={environments.length > 0}
                onDeployGateway={handleDeployGateway}
            />

            {!customer && <CloudTrialCountdown daysRemaining={trialDaysRemaining} />}

            <CloudLearnAboutSection />
        </div>
    );
}
