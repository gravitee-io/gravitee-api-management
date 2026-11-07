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
import '@gravitee/gamma-lib-observability/styles';

import {
    AdditionalMetricsCatalog,
    createHttpMetricDefinitionsSource,
    MetricDefinitionsSourceProvider,
} from '@gravitee/gamma-lib-observability';
import { gammaConsoleHttpOptions } from '@gravitee/gamma-ui-shared/api';
import { useObservabilityBaseUrl } from '@gravitee/gamma-ui-shared/observability';
import { TooltipProvider } from '@gravitee/graphene-core';
import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { useMemo, useState } from 'react';

import { useEnvironment, useHasPermission } from '@gravitee/gamma-modules-sdk';

import { ROUTES } from '../config/routes';
import { retryTransientRequest } from '../shared/api/queryRetry';
import { SectionError } from '../shared/components/SectionError';

const TITLE = ROUTES['environment-metrics-configuration'].label;
const DESCRIPTION =
    'Fields discovered in this environment and fields you declared. Labels, units and exposure apply to every Observability screen of the environment.';

/**
 * The catalog's queries and mutations run on their own client, not the module's: the module retries
 * a failed mutation twice, and a declaration is a POST, so one retried after a lost answer is refused
 * as a duplicate though the record exists. Reads keep the module's retry rule. The client lives with
 * the mounted environment, so a switch never shows another environment's cached rows.
 */
function createCatalogQueryClient(): QueryClient {
    return new QueryClient({
        defaultOptions: {
            queries: { retry: retryTransientRequest },
            mutations: { retry: false },
        },
    });
}

/**
 * Environment-scoped metrics configuration: `platform > environment > Metrics configuration`.
 *
 * Mounts the observability library's additional metrics catalog on the Gamma API of the current
 * environment. The catalog saves per sheet, so the page has no Save bar. Edit controls need
 * `environment-settings-u`, what the Gamma API asks of a write; a reader gets the table only. The list
 * needs a login only, so unlike the sibling settings pages this one has no read to redirect on when
 * refused: the catalog reports a refused call where it happens.
 * Discovered rows and "Last seen" are served to a reader of the environment's traffic only
 * (`ENVIRONMENT_PLATFORM[READ]`, or API analytics / log read on one of its APIs): the table shows
 * what the server lists, so an environment-settings admin without that right sees the built-in and
 * declared rows, with no "Last seen".
 */
export function MetricsConfigurationPage() {
    const env = useEnvironment();
    // The base URL carries the environment id: nothing is built, or fetched, before the host reports it.
    if (!env?.id) return null;
    return <EnvironmentMetricsConfiguration key={env.id} />;
}

function EnvironmentMetricsConfiguration() {
    const canEdit = useHasPermission({ anyOf: ['environment-settings-u'] });
    const { baseUrl, isError } = useObservabilityBaseUrl();
    const [catalogQueryClient] = useState(createCatalogQueryClient);

    const source = useMemo(
        () => (baseUrl === undefined ? undefined : createHttpMetricDefinitionsSource(baseUrl, gammaConsoleHttpOptions)),
        [baseUrl],
    );

    if (!isError && source === undefined) return null;

    return (
        <TooltipProvider delayDuration={200}>
            <div className="space-y-6">
                <div className="space-y-1">
                    <h1 className="text-2xl font-semibold tracking-tight">{TITLE}</h1>
                    <p className="text-sm text-muted-foreground">{DESCRIPTION}</p>
                </div>
                {source === undefined ? (
                    <SectionError message="Failed to load the environment configuration. Please refresh and try again." />
                ) : (
                    <QueryClientProvider client={catalogQueryClient}>
                        <MetricDefinitionsSourceProvider source={source}>
                            <AdditionalMetricsCatalog mode={canEdit ? 'edit' : 'read'} />
                        </MetricDefinitionsSourceProvider>
                    </QueryClientProvider>
                )}
            </div>
        </TooltipProvider>
    );
}
