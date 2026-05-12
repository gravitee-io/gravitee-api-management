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
import { useModuleRouting } from '@gravitee/gamma-modules-sdk/routing';
import { Badge, Separator, Skeleton } from '@gravitee/graphene-core';
import { Outlet, useNavigate, useParams } from 'react-router-dom';

import { APIM_ROUTE_CONFIG } from '../../../config/routes';
import { ApiDetailSidebarNav } from '../components/ApiDetailSidebarNav';
import { ApiDetailContext } from '../context/ApiDetailContext';
import { useApiDetail } from '../hooks/useApiDetail';

export function ApiDetailLayout() {
    const { apiId } = useParams<{ apiId: string }>();
    const navigate = useNavigate();
    const { rootPath } = useModuleRouting(APIM_ROUTE_CONFIG);
    const { data: api, isLoading } = useApiDetail(apiId);

    const started = api?.state === 'STARTED';

    return (
        <ApiDetailContext.Provider value={{ api: api ?? null, isLoading }}>
            <div className="flex flex-col">
                {/* Fixed header — does not scroll */}
                <div className="pb-6 space-y-3 shrink-0">
                    <div className="flex items-start gap-3">
                        {isLoading ? (
                            <Skeleton className="h-7 w-64" />
                        ) : (
                            <h1 className="text-2xl font-semibold tracking-tight leading-none">{api?.name ?? apiId}</h1>
                        )}
                        {!isLoading && api ? (
                            <Badge variant={started ? 'default' : 'secondary'} className="shrink-0 mt-0.5">
                                {started ? 'Started' : 'Stopped'}
                            </Badge>
                        ) : null}
                    </div>

                    {isLoading ? (
                        <Skeleton className="h-4 w-96" />
                    ) : api?.description ? (
                        <p className="text-sm text-muted-foreground">{api.description}</p>
                    ) : null}

                    {!isLoading && api ? (
                        <div className="flex items-center gap-2 flex-wrap">
                            <Badge variant="outline" className="font-normal text-xs">
                                {api.type ?? 'PROXY'}
                            </Badge>
                            <Badge variant="outline" className="font-normal text-xs">
                                v{api.apiVersion ?? '—'}
                            </Badge>
                            <Badge variant="outline" className="font-normal text-xs">
                                {api.definitionVersion ?? 'V4'}
                            </Badge>
                        </div>
                    ) : null}
                </div>

                <Separator className="shrink-0" />

                {/* Two-column body */}
                <div className="flex gap-6 pt-4">
                    <ApiDetailSidebarNav onBack={() => navigate(rootPath)} />
                    <main className="min-w-0 flex-1 py-6">
                        <Outlet />
                    </main>
                </div>
            </div>
        </ApiDetailContext.Provider>
    );
}
