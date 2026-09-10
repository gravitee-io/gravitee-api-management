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

/** Skeleton height, inline: one group of the panel is about this tall. */
const ROW_HEIGHT = { height: 208 } as const;

/**
 * The "Targets" tab: the shared panel mounted on this API as its own subject.
 * The panel reads what the API's own telemetry saw, so a rule can be narrowed
 * to the paths and methods it actually served. Targets are environment
 * resources on the REST side, so the writes follow the environment API
 * permissions rather than the API-scoped ones.
 */
export function ApiTargetsPage() {
    const { api } = useApiDetailContext();
    const canManage = useHasPermission({ anyOf: ['environment-api-c', 'environment-api-u'] });

    return (
        <div className="flex min-w-0 flex-col gap-6">
            <div className="min-w-0 space-y-1">
                <h1 className="text-2xl font-semibold tracking-tight">Targets</h1>
                <p className="text-muted-foreground max-w-prose text-sm">The thresholds this API is held to.</p>
            </div>
            {api ? (
                <ApiTargetsProvider>
                    {/* The module only manages V4 HTTP proxies (`PROXY`), which the list search enforces server-side. */}
                    <TargetsPanel
                        reference={api.id}
                        apiIds={[api.id]}
                        apiTypes={[api.type ?? 'PROXY']}
                        readOnly={!canManage}
                        emptyDescription="Nothing is watching this API yet."
                    />
                </ApiTargetsProvider>
            ) : (
                <Skeleton className="rounded-xl" style={ROW_HEIGHT} />
            )}
        </div>
    );
}
