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
import {
    Alert,
    AlertDescription,
    Badge,
    Button,
    Card,
    CardContent,
    CardDescription,
    CardHeader,
    CardTitle,
    Input,
    Label,
    Select,
    SelectContent,
    SelectItem,
    SelectTrigger,
    SelectValue,
} from '@gravitee/graphene-core';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { useEffect, useMemo, useState } from 'react';

import { useEnvironmentId } from '../../../lib/hooks/useEnvironmentId';
import { searchApis } from '../../apis/services/apiList';
import {
    type RuntimeOverlay,
    type RuntimeOverlayStatus,
    liveStatus,
    listRuntimeOverlays,
    startRuntimeOverlay,
    stopRuntimeOverlay,
} from '../services/runtimeOverlays';

const DEMO_KIND = 'demo';
const POLL_INTERVAL_MS = 2_000;

const STATUS_BADGE: Record<RuntimeOverlayStatus, 'success' | 'secondary' | 'outline'> = {
    ACTIVE: 'success',
    STOPPED: 'secondary',
    EXPIRED: 'outline',
};

function useNow(): number {
    const [now, setNow] = useState(() => Date.now());
    useEffect(() => {
        const timer = setInterval(() => setNow(Date.now()), 250);
        return () => clearInterval(timer);
    }, []);
    return now;
}

export function RuntimeOverlaysPage() {
    const environmentId = useEnvironmentId();
    const queryClient = useQueryClient();
    const now = useNow();

    const [apiId, setApiId] = useState('');
    const [durationSeconds, setDurationSeconds] = useState('60');
    const [message, setMessage] = useState('Hello from a runtime overlay');

    const apisQuery = useQuery({
        queryKey: ['runtime-overlays', 'apis', environmentId],
        queryFn: () => searchApis(environmentId, {}, 1, 100, 'name'),
    });
    const apiNames = useMemo(() => new Map((apisQuery.data?.data ?? []).map(api => [api.id, api.name])), [apisQuery.data]);

    const overlaysQueryKey = ['runtime-overlays', 'list', environmentId];
    const overlaysQuery = useQuery({
        queryKey: overlaysQueryKey,
        queryFn: () => listRuntimeOverlays(environmentId),
        refetchInterval: POLL_INTERVAL_MS,
    });

    const startMutation = useMutation({
        mutationFn: () =>
            startRuntimeOverlay(environmentId, {
                apiId,
                kind: DEMO_KIND,
                durationSeconds: Number(durationSeconds),
                payload: { message },
            }),
        onSuccess: () => queryClient.invalidateQueries({ queryKey: overlaysQueryKey }),
    });

    const stopMutation = useMutation({
        mutationFn: (overlay: RuntimeOverlay) => stopRuntimeOverlay(environmentId, overlay),
        onSuccess: () => queryClient.invalidateQueries({ queryKey: overlaysQueryKey }),
    });

    const canStart = apiId !== '' && Number(durationSeconds) > 0 && !startMutation.isPending;

    return (
        <div className="space-y-6">
            <div className="space-y-1">
                <h1 className="text-2xl font-semibold tracking-tight">Runtime overlays</h1>
                <p className="text-sm text-muted-foreground">
                    Change the behavior of a deployed API for a bounded time, without redeploying it. Gateways pick a runtime overlay up at
                    their next sync, within a few seconds.
                </p>
            </div>

            <Card>
                <CardHeader>
                    <CardTitle>Start a demo runtime overlay</CardTitle>
                    <CardDescription>
                        While it is active, every call to the API is tagged: the gateway logs it and answers with an X-Runtime-Overlay
                        header carrying the call count.
                    </CardDescription>
                </CardHeader>
                <CardContent>
                    <form
                        className="grid gap-4 md:grid-cols-[2fr_1fr_3fr_auto] md:items-end"
                        onSubmit={event => {
                            event.preventDefault();
                            if (canStart) startMutation.mutate();
                        }}
                    >
                        <div className="space-y-2">
                            <Label htmlFor="runtime-overlay-api">API</Label>
                            <Select value={apiId} onValueChange={setApiId}>
                                <SelectTrigger id="runtime-overlay-api">
                                    <SelectValue placeholder={apisQuery.isLoading ? 'Loading APIs…' : 'Select an API'} />
                                </SelectTrigger>
                                <SelectContent>
                                    {(apisQuery.data?.data ?? []).map(api => (
                                        <SelectItem key={api.id} value={api.id}>
                                            {api.name}
                                        </SelectItem>
                                    ))}
                                </SelectContent>
                            </Select>
                        </div>
                        <div className="space-y-2">
                            <Label htmlFor="runtime-overlay-duration">Duration (seconds)</Label>
                            <Input
                                id="runtime-overlay-duration"
                                type="number"
                                min={1}
                                max={3600}
                                value={durationSeconds}
                                onChange={event => setDurationSeconds(event.target.value)}
                            />
                        </div>
                        <div className="space-y-2">
                            <Label htmlFor="runtime-overlay-message">Message</Label>
                            <Input id="runtime-overlay-message" value={message} onChange={event => setMessage(event.target.value)} />
                        </div>
                        <Button type="submit" disabled={!canStart}>
                            {startMutation.isPending ? 'Starting…' : 'Start'}
                        </Button>
                    </form>
                    {startMutation.isError && (
                        <Alert variant="destructive" className="mt-4">
                            <AlertDescription>Failed to start the runtime overlay: {startMutation.error.message}</AlertDescription>
                        </Alert>
                    )}
                </CardContent>
            </Card>

            <Card>
                <CardHeader>
                    <CardTitle>Runtime overlays of the last hour</CardTitle>
                    <CardDescription>Refreshed every {POLL_INTERVAL_MS / 1000} seconds.</CardDescription>
                </CardHeader>
                <CardContent className="space-y-3">
                    {overlaysQuery.isError && (
                        <Alert variant="destructive">
                            <AlertDescription>Failed to load runtime overlays: {overlaysQuery.error.message}</AlertDescription>
                        </Alert>
                    )}
                    {overlaysQuery.data?.length === 0 && <p className="text-sm text-muted-foreground">No runtime overlay yet.</p>}
                    {(overlaysQuery.data ?? []).map(overlay => (
                        <RuntimeOverlayRow
                            key={overlay.id}
                            overlay={overlay}
                            apiName={apiNames.get(overlay.apiId) ?? overlay.apiId}
                            now={now}
                            stopping={stopMutation.isPending && stopMutation.variables?.id === overlay.id}
                            onStop={() => stopMutation.mutate(overlay)}
                        />
                    ))}
                </CardContent>
            </Card>
        </div>
    );
}

interface RuntimeOverlayRowProps {
    overlay: RuntimeOverlay;
    apiName: string;
    now: number;
    stopping: boolean;
    onStop: () => void;
}

function RuntimeOverlayRow({ overlay, apiName, now, stopping, onStop }: RuntimeOverlayRowProps) {
    const status = liveStatus(overlay, now);
    const total = overlay.endAt - overlay.startAt;
    const remaining = Math.max(0, overlay.endAt - now);
    const progress = status === 'ACTIVE' && total > 0 ? (remaining / total) * 100 : 0;
    const message = typeof overlay.payload.message === 'string' ? overlay.payload.message : undefined;

    return (
        <div className="space-y-2 rounded-md border p-3">
            <div className="flex flex-wrap items-center gap-2">
                <span className="font-medium">{apiName}</span>
                <Badge variant="outline">{overlay.kind}</Badge>
                <Badge variant={STATUS_BADGE[status]}>{status}</Badge>
                <span className="text-sm text-muted-foreground">
                    {status === 'ACTIVE'
                        ? `${Math.ceil(remaining / 1000)} s left`
                        : `ended ${new Date(overlay.endAt).toLocaleTimeString()}`}
                </span>
                <span className="ml-auto font-mono text-xs text-muted-foreground">{overlay.id}</span>
                {status === 'ACTIVE' && (
                    <Button variant="outline" size="sm" disabled={stopping} onClick={onStop}>
                        {stopping ? 'Stopping…' : 'Stop'}
                    </Button>
                )}
            </div>
            {message && <p className="text-sm">“{message}”</p>}
            <div className="h-1.5 w-full overflow-hidden rounded bg-muted">
                <div className="h-full bg-primary transition-[width] duration-200" style={{ width: `${progress}%` }} />
            </div>
        </div>
    );
}
