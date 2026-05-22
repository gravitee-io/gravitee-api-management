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
import { Badge, Button, Card, CardContent, Skeleton } from '@gravitee/graphene-core';
import { ArrowRightIcon, TriangleAlertIcon } from '@gravitee/graphene-core/icons';

import type { ApiDeploymentState, ApiListItem, ApiState } from '../../apis/types/api';
import { useDashboardRecentApis } from '../hooks/useDashboardRecentApis';

// ─── State helpers ────────────────────────────────────────────────────────────

function DeploymentBadge({ state }: { state?: ApiDeploymentState }) {
    if (state === 'NEED_REDEPLOY') {
        return (
            <Badge variant="outline" className="text-warning border-warning/30 shrink-0 gap-1">
                <TriangleAlertIcon className="size-3" aria-hidden />
                Needs redeploy
            </Badge>
        );
    }
    if (state === 'DEPLOYED') {
        return (
            <Badge variant="outline" className="text-success border-success/20 shrink-0">
                Deployed
            </Badge>
        );
    }
    return null;
}

function ApiStateDot({ state }: { state?: ApiState }) {
    const isRunning = state === 'STARTED';
    return (
        <span
            className={`size-2 rounded-full shrink-0 ${isRunning ? 'bg-success' : 'bg-muted-foreground/40'}`}
            aria-label={isRunning ? 'Running' : 'Stopped'}
        />
    );
}

// ─── Row ──────────────────────────────────────────────────────────────────────

interface ApiRowProps {
    api: ApiListItem;
    onClick: () => void;
}

function ApiRow({ api, onClick }: ApiRowProps) {
    return (
        <button
            type="button"
            onClick={onClick}
            className="w-full flex items-center gap-3 px-3 py-2.5 rounded-lg hover:bg-muted transition-colors text-left"
        >
            <ApiStateDot state={api.state} />
            <div className="flex-1 min-w-0">
                <p className="text-sm font-medium truncate">{api.name}</p>
                <p className="text-xs text-muted-foreground truncate">v{api.apiVersion}</p>
            </div>
            <DeploymentBadge state={api.deploymentState} />
        </button>
    );
}

// ─── Skeleton ─────────────────────────────────────────────────────────────────

function RecentActivitySkeleton() {
    return (
        <div className="space-y-1 px-3 py-1">
            {Array.from({ length: 5 }).map((_, i) => (
                <div key={i} className="flex items-center gap-3 py-2.5">
                    <Skeleton className="size-2 rounded-full shrink-0" />
                    <div className="flex-1 space-y-1">
                        <Skeleton className="h-4 w-40 rounded" />
                        <Skeleton className="h-3 w-16 rounded" />
                    </div>
                    <Skeleton className="h-5 w-20 rounded" />
                </div>
            ))}
        </div>
    );
}

// ─── Public component ─────────────────────────────────────────────────────────

interface DashboardRecentActivityProps {
    onNavigateToApi: (apiId: string) => void;
    onGoToApis: () => void;
}

export function DashboardRecentActivity({ onNavigateToApi, onGoToApis }: DashboardRecentActivityProps) {
    const { apis, isLoading, isError } = useDashboardRecentApis();

    return (
        <Card className="flex flex-col">
            <CardContent className="pt-5 pb-4 flex flex-col gap-3 h-full">
                <div className="flex items-center justify-between">
                    <div>
                        <p className="text-sm font-semibold">Your APIs</p>
                        <p className="text-xs text-muted-foreground">APIs you have access to manage</p>
                    </div>
                </div>

                {isLoading && <RecentActivitySkeleton />}

                {isError && <p className="text-xs text-muted-foreground px-3 py-4">Unable to load recent APIs.</p>}

                {!isLoading && !isError && apis.length === 0 && <p className="text-xs text-muted-foreground px-3 py-4">No APIs found.</p>}

                {!isLoading && !isError && apis.length > 0 && (
                    <div className="space-y-0.5">
                        {apis.map(api => (
                            <ApiRow key={api.id} api={api} onClick={() => onNavigateToApi(api.id)} />
                        ))}
                    </div>
                )}

                <div className="border-t pt-3 mt-auto">
                    <Button variant="ghost" size="sm" onClick={onGoToApis} className="w-full justify-between">
                        View all APIs
                        <ArrowRightIcon className="size-4" aria-hidden />
                    </Button>
                </div>
            </CardContent>
        </Card>
    );
}
