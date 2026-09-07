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
import { TargetsPanel } from '@gravitee/gamma-lib-observability';
import { useHasPermission } from '@gravitee/gamma-modules-sdk';
import { Skeleton } from '@gravitee/graphene-core';

import { ApiTargetsProvider } from '../../../components/targets/ApiTargetsProvider';
import { useApiDetailContext } from '../../../context/ApiDetailContext';

/**
 * The "Targets" tab: the shared panel mounted on this API as its own subject.
 * Targets are environment resources on the REST side, so the writes follow the
 * environment API permissions rather than the API-scoped ones.
 */
export function ApiTargetsPage() {
    const { api } = useApiDetailContext();
    const canManage = useHasPermission({ anyOf: ['environment-api-c', 'environment-api-u'] });

    return (
        <div className="space-y-6">
            <div className="space-y-1">
                <h1 className="text-2xl font-semibold tracking-tight">Targets</h1>
                <p className="text-sm text-muted-foreground">
                    What good looks like for this API: rules over its gateway telemetry, evaluated continuously against the thresholds you
                    declare.
                </p>
            </div>
            {api ? (
                <ApiTargetsProvider>
                    {/* The module only manages V4 HTTP proxies (`PROXY`), which the list search enforces server-side. */}
                    <TargetsPanel reference={api.id} apiIds={[api.id]} apiTypes={[api.type ?? 'PROXY']} readOnly={!canManage} />
                </ApiTargetsProvider>
            ) : (
                <Skeleton className="h-40 w-full" />
            )}
        </div>
    );
}
