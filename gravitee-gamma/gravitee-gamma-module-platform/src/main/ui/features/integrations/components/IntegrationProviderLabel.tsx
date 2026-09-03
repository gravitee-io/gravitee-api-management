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

import { useEffect } from 'react';

import { hasProviderLabel, integrationProviderLabel, SUPPORTED_PROVIDER_TOKENS } from '../utils/providerLabels';

export function IntegrationProviderLabel({ provider }: Readonly<{ provider: string }>) {
    const unmappedProvider = hasProviderLabel(provider) ? undefined : provider;
    useEffect(() => {
        if (unmappedProvider) {
            console.warn(
                `Integration provider "${unmappedProvider}" is not one of ${SUPPORTED_PROVIDER_TOKENS.join(', ')}; rendering the raw provider key`,
            );
        }
    }, [unmappedProvider]);

    return <span className="text-sm">{integrationProviderLabel(provider)}</span>;
}
