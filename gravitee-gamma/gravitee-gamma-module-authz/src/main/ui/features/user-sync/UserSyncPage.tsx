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
import { useEnvironment } from '@gravitee/gamma-modules-sdk';
import { Alert, AlertDescription, AlertTitle, Badge, Button, Card, Spinner } from '@gravitee/graphene-core';
import type { ComponentProps } from 'react';
import type { AmSyncStatus } from '../../shared/api/authz-api.types';
import { useUserSync } from '../../shared/hooks/useUserSync';

type BadgeVariant = NonNullable<ComponentProps<typeof Badge>['variant']>;

const STATUS_PRESENTATION: Record<AmSyncStatus, { readonly label: string; readonly variant: BadgeVariant }> = {
    RUNNING: { label: 'Running', variant: 'highlight' },
    COMPLETED: { label: 'Completed', variant: 'success' },
    FAILED: { label: 'Failed', variant: 'destructive' },
};

export function UserSyncPage() {
    const env = useEnvironment();
    const environmentId = env?.id ?? '';
    const { status, isLoadingStatus, statusError, start, isStarting, startError } = useUserSync(environmentId);

    const isRunning = status?.status === 'RUNNING';

    const onSync = () => {
        // A 409 (sync already running) rejects the mutation; the hook suppresses it and
        // the status card reflects the in-flight job, so swallow the rejection here.
        void start().catch(() => {});
    };

    return (
        <div className="flex flex-col gap-6">
            <header className="flex flex-col gap-2">
                <h1 className="text-2xl font-semibold">AM User Sync</h1>
                <p className="max-w-3xl text-sm text-muted-foreground">
                    Pull every user from the configured Access Management domain and upsert them as{' '}
                    <span className="font-medium text-foreground">PRINCIPAL</span> entities, keyed on the token{' '}
                    <span className="font-medium text-foreground">sub</span> so policy decisions match at the gateway.
                </p>
            </header>

            <Card className="flex flex-col gap-4 p-5">
                <div className="flex items-center gap-3">
                    <Button onClick={onSync} disabled={isStarting || isRunning}>
                        {isStarting ? 'Starting…' : 'Sync users'}
                    </Button>
                    {isRunning ? (
                        <span className="flex items-center gap-2 text-sm text-muted-foreground">
                            <Spinner className="size-4" /> A sync is currently running
                        </span>
                    ) : null}
                </div>

                {startError ? (
                    <Alert variant="destructive">
                        <AlertTitle>Could not start sync</AlertTitle>
                        <AlertDescription>{startError}</AlertDescription>
                    </Alert>
                ) : null}
            </Card>

            <Card className="flex flex-col gap-3 p-5">
                <div className="flex items-center justify-between">
                    <h2 className="text-lg font-semibold">Latest sync</h2>
                    {status ? <Badge variant={STATUS_PRESENTATION[status.status].variant}>{STATUS_PRESENTATION[status.status].label}</Badge> : null}
                </div>

                {isLoadingStatus ? (
                    <span className="flex items-center gap-2 text-sm text-muted-foreground">
                        <Spinner className="size-4" /> Loading status…
                    </span>
                ) : statusError ? (
                    <Alert variant="destructive">
                        <AlertTitle>Could not load status</AlertTitle>
                        <AlertDescription>{statusError}</AlertDescription>
                    </Alert>
                ) : !status ? (
                    <p className="text-sm text-muted-foreground">No sync has run for this organization yet.</p>
                ) : (
                    <div className="flex flex-col gap-3">
                        <div className="grid gap-4" style={{ gridTemplateColumns: 'repeat(auto-fit, minmax(160px, 1fr))' }}>
                            <div className="flex flex-col gap-0.5">
                                <div className="text-3xl font-semibold leading-none">{status.usersFetched}</div>
                                <div className="text-sm text-muted-foreground">Users fetched</div>
                            </div>
                            <div className="flex flex-col gap-0.5">
                                <div className="text-3xl font-semibold leading-none">{status.entitiesUpserted}</div>
                                <div className="text-sm text-muted-foreground">Entities upserted</div>
                            </div>
                        </div>
                        {status.error ? (
                            <Alert variant="destructive">
                                <AlertTitle>Sync failed</AlertTitle>
                                <AlertDescription>{status.error}</AlertDescription>
                            </Alert>
                        ) : null}
                        {status.completedAt ? (
                            <p className="text-xs text-muted-foreground">Completed at {new Date(status.completedAt).toLocaleString()}</p>
                        ) : null}
                    </div>
                )}
            </Card>
        </div>
    );
}
