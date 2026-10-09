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

import { useId } from 'react';

import { IntegrationProviderLabel } from './IntegrationProviderLabel';
import { ProviderMonogram } from './ProviderMonogram';
import { findProvider } from '../utils/providerLabels';

export function IntegrationProviderField({ provider }: Readonly<{ provider: string }>) {
    const headingId = useId();
    const monogram = findProvider(provider)?.monogram ?? provider.slice(0, 3).toUpperCase();

    return (
        <section className="min-w-0 flex-1 space-y-2" data-testid="integration-provider" aria-labelledby={headingId}>
            <h2 id={headingId} className="text-sm font-semibold">
                Provider
            </h2>
            <div className="flex items-center gap-2 font-semibold">
                <ProviderMonogram monogram={monogram} />
                <IntegrationProviderLabel provider={provider} />
            </div>
        </section>
    );
}
