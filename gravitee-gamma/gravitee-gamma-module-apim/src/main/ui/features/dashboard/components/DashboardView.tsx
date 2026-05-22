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
import { DashboardGetStartedCards } from './DashboardGetStartedCards';
import { DashboardQuickActions } from './DashboardQuickActions';
import { DashboardSummaryCards } from './DashboardSummaryCards';

interface DashboardViewProps {
    totalApis: number | null;
    totalProducts: number | null;
    onCreateProxy: () => void;
    onCreateProduct: () => void;
    onGoToApis: () => void;
    onGoToApiProducts: () => void;
    onGoToAnalytics: () => void;
}

export function DashboardView({
    totalApis,
    totalProducts,
    onCreateProxy,
    onCreateProduct,
    onGoToApis,
    onGoToApiProducts,
    onGoToAnalytics,
}: DashboardViewProps) {
    return (
        <div className="space-y-6">
            {/* Header */}
            <div className="space-y-1">
                <h1 className="text-2xl font-semibold tracking-tight">API Management</h1>
                <p className="text-sm text-muted-foreground">Manage, secure, and monitor your APIs &amp; API Products</p>
            </div>

            {/* Summary stats */}
            <DashboardSummaryCards totalApis={totalApis} totalProducts={totalProducts} />

            {/* Quick actions */}
            <DashboardQuickActions onGoToApis={onGoToApis} onGoToApiProducts={onGoToApiProducts} onGoToAnalytics={onGoToAnalytics} />

            {/* Get Started */}
            <section>
                <h2 className="text-base font-semibold mb-4">Get Started</h2>
                <DashboardGetStartedCards onCreateProxy={onCreateProxy} onCreateProduct={onCreateProduct} />
            </section>
        </div>
    );
}
