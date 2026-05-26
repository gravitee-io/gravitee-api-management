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
import { useEnvironment, useHasPermission } from '@gravitee/gamma-modules-sdk';
import { Button, Card, CardContent, CardHeader, CardTitle, Input, Label, Skeleton, Switch, TooltipProvider } from '@gravitee/graphene-core';
import { GlobeIcon, TriangleAlertIcon } from '@gravitee/graphene-core/icons';
import { useMutation, useQueryClient } from '@tanstack/react-query';
import { useEffect, useMemo, useRef, useState } from 'react';
import { useParams } from 'react-router-dom';

import { Chips } from './Chips';
import { InfoTooltip } from './InfoTooltip';
import { ToggleRow } from './ToggleRow';
import { useApiDetail } from '../../../hooks/useApiDetail';
import { updateApiCors } from '../../../services/apis';
import type { Cors } from '../../../types';
import { apiDetailKeys } from '../../../utils/queryKeys';

// ─── Constants ────────────────────────────────────────────────────────────────

const HTTP_METHODS = ['GET', 'POST', 'PUT', 'PATCH', 'DELETE', 'HEAD', 'OPTIONS'];

const COMMON_ALLOW_HEADERS = ['Accept', 'Authorization', 'Content-Type', 'Origin', 'X-API-Key', 'X-Requested-With'];

const COMMON_EXPOSE_HEADERS = ['Content-Length', 'Content-Range', 'X-Request-Id'];

// ─── Page ─────────────────────────────────────────────────────────────────────

export function ApiCorsPage() {
    const { apiId } = useParams<{ apiId: string }>();
    const env = useEnvironment();
    const queryClient = useQueryClient();

    const canEdit = useHasPermission({ anyOf: ['api-definition-u'] });
    const { data: api, isLoading, isError } = useApiDetail(apiId);

    const savedCors = useMemo(() => api?.listeners?.find(l => l.type === 'HTTP')?.cors ?? null, [api?.listeners]);

    // ─── Form state ───────────────────────────────────────────────────────────
    const [enabled, setEnabled] = useState(false);
    const [origins, setOrigins] = useState<string[]>([]);
    const [methods, setMethods] = useState<string[]>([]);
    const [allowHeaders, setAllowHeaders] = useState<string[]>([]);
    const [exposeHeaders, setExposeHeaders] = useState<string[]>([]);
    const [allowCredentials, setAllowCredentials] = useState(false);
    const [runPolicies, setRunPolicies] = useState(false);
    const [maxAge, setMaxAge] = useState('');
    const [saveError, setSaveError] = useState<string | null>(null);

    // Initialize form once per apiId — prevents background refetches from wiping in-progress edits
    const initializedForApiIdRef = useRef<string | undefined>(undefined);

    useEffect(() => {
        if (!savedCors || initializedForApiIdRef.current === apiId) return;
        initializedForApiIdRef.current = apiId;
        setEnabled(savedCors.enabled ?? false);
        setOrigins(savedCors.allowOrigin ?? []);
        setMethods(savedCors.allowMethods ?? []);
        setAllowHeaders(savedCors.allowHeaders ?? []);
        setExposeHeaders(savedCors.exposeHeaders ?? []);
        setAllowCredentials(savedCors.allowCredentials ?? false);
        setRunPolicies(savedCors.runPolicies ?? false);
        setMaxAge(savedCors.maxAge !== undefined ? String(savedCors.maxAge) : '');
    }, [savedCors, apiId]);

    // ─── Mutation ─────────────────────────────────────────────────────────────
    const mutation = useMutation({
        mutationFn: (cors: Cors) => updateApiCors(env!.id, apiId!, cors),
        onSuccess: () => {
            queryClient.invalidateQueries({ queryKey: apiDetailKeys.detail(env?.id ?? '', apiId ?? '') });
            setSaveError(null);
        },
        onError: (e: Error) => {
            setSaveError(e.message || 'Failed to save CORS settings.');
        },
    });

    const handleSave = () => {
        const parsedMaxAge = maxAge.trim() !== '' ? Number(maxAge) : undefined;
        mutation.mutate({
            enabled,
            allowOrigin: origins,
            allowMethods: methods,
            allowHeaders,
            exposeHeaders,
            allowCredentials,
            runPolicies,
            ...(parsedMaxAge !== undefined && !Number.isNaN(parsedMaxAge) ? { maxAge: parsedMaxAge } : {}),
        });
    };

    const fieldsDisabled = !enabled;

    // ─── Loading ──────────────────────────────────────────────────────────────
    if (isLoading) {
        return (
            <div className="space-y-6 p-6">
                <div className="flex items-start justify-between gap-4">
                    <div className="space-y-2">
                        <Skeleton className="h-8 w-24 rounded" />
                        <Skeleton className="h-4 w-64 rounded" />
                    </div>
                    <Skeleton className="h-8 w-28 rounded" />
                </div>
                <Skeleton className="h-20 w-full rounded-lg" />
                <Skeleton className="h-64 w-full rounded-lg" />
                <Skeleton className="h-48 w-full rounded-lg" />
            </div>
        );
    }

    if (isError) {
        return (
            <div className="p-6">
                <Card className="border-destructive/30">
                    <CardContent className="pt-4 pb-4">
                        <p className="text-sm text-destructive">Failed to load CORS settings. Please try again.</p>
                    </CardContent>
                </Card>
            </div>
        );
    }

    return (
        <TooltipProvider>
            <div className="space-y-6 p-6">
                {/* ─── Header ─────────────────────────────────────────────────── */}
                <div className="flex items-start justify-between gap-4">
                    <div className="space-y-1">
                        <h1 className="text-2xl font-semibold">CORS</h1>
                        <p className="text-sm text-muted-foreground">Configure cross-origin access for this API.</p>
                    </div>
                    {canEdit && (
                        <Button size="sm" onClick={handleSave} disabled={mutation.isPending}>
                            {mutation.isPending ? 'Saving…' : 'Save changes'}
                        </Button>
                    )}
                </div>

                {/* ─── Save error ──────────────────────────────────────────────── */}
                {saveError && (
                    <div
                        className="rounded-lg p-3"
                        style={{ background: 'hsl(var(--destructive) / 0.08)', border: '1px solid hsl(var(--destructive) / 0.3)' }}
                    >
                        <p className="text-xs text-destructive">{saveError}</p>
                    </div>
                )}

                {/* ─── Enable CORS ─────────────────────────────────────────────── */}
                <Card>
                    <CardHeader>
                        <div className="flex items-start justify-between gap-4">
                            <CardTitle className="flex items-center gap-2 text-base">
                                <GlobeIcon className="size-4 text-primary" />
                                Enable CORS
                                <InfoTooltip
                                    content={
                                        <>
                                            When enabled the gateway adds <span className="font-mono text-xs">Access-Control-*</span>{' '}
                                            headers and short-circuits preflight requests.
                                        </>
                                    }
                                />
                            </CardTitle>
                            <Switch checked={enabled} onCheckedChange={setEnabled} disabled={!canEdit} />
                        </div>
                    </CardHeader>
                </Card>

                {/* ─── Origins, methods and headers ────────────────────────────── */}
                <Card>
                    <CardHeader>
                        <CardTitle className="flex items-center gap-2 text-base">
                            Origins, methods and headers
                            <InfoTooltip
                                content={
                                    <>
                                        Use <span className="font-mono text-xs">*</span> to allow any value. Regular expressions are
                                        supported for origins.
                                    </>
                                }
                            />
                        </CardTitle>
                    </CardHeader>
                    <CardContent className="space-y-6">
                        <Chips
                            label="Access-Control-Allow-Origin"
                            hint="Origins that may call this API. Scheme, domain and port must match."
                            values={origins}
                            disabled={fieldsDisabled || !canEdit}
                            placeholder="https://app.example.com or *"
                            onChange={setOrigins}
                        />

                        {origins.includes('*') && (
                            <div
                                className="flex gap-2.5 rounded-lg p-3"
                                style={{
                                    background: 'hsl(var(--destructive) / 0.08)',
                                    border: '1px solid hsl(var(--destructive) / 0.3)',
                                }}
                            >
                                <TriangleAlertIcon className="size-4 shrink-0 text-destructive mt-0.5" />
                                <div>
                                    <p className="text-xs font-semibold text-destructive">Allowing all origins</p>
                                    <p className="text-xs text-muted-foreground mt-0.5">
                                        Setting <span className="font-mono">*</span> exposes this API to any website. Make sure that&apos;s
                                        intentional and that authentication is enforced.
                                    </p>
                                </div>
                            </div>
                        )}

                        <Chips
                            label="Access-Control-Allow-Methods"
                            hint="HTTP methods that may be used in cross-origin requests."
                            values={methods}
                            disabled={fieldsDisabled || !canEdit}
                            suggestions={HTTP_METHODS}
                            placeholder="GET, POST, …"
                            onChange={setMethods}
                        />

                        <Chips
                            label="Access-Control-Allow-Headers"
                            hint="Headers the client is allowed to send in the actual request."
                            values={allowHeaders}
                            disabled={fieldsDisabled || !canEdit}
                            suggestions={COMMON_ALLOW_HEADERS}
                            placeholder="Content-Type, Authorization, …"
                            onChange={setAllowHeaders}
                        />

                        <Chips
                            label="Access-Control-Expose-Headers"
                            hint="Headers from the response that the browser is allowed to surface to JavaScript."
                            values={exposeHeaders}
                            disabled={fieldsDisabled || !canEdit}
                            suggestions={COMMON_EXPOSE_HEADERS}
                            placeholder="X-Request-Id, …"
                            onChange={setExposeHeaders}
                        />
                    </CardContent>
                </Card>

                {/* ─── Advanced settings ────────────────────────────────────────── */}
                <Card>
                    <CardHeader>
                        <CardTitle className="flex items-center gap-2 text-base">
                            Advanced settings
                            <InfoTooltip content="Fine-tune credentials handling, preflight cache duration and how policies are evaluated." />
                        </CardTitle>
                    </CardHeader>
                    <CardContent className="space-y-6">
                        <ToggleRow
                            id="cors-allow-credentials"
                            label="Access-Control-Allow-Credentials"
                            description="Allow browsers to include cookies and HTTP authentication."
                            hint={
                                <>
                                    Not compatible with <span className="font-mono text-xs">*</span> as the allowed origin.
                                </>
                            }
                            checked={allowCredentials}
                            disabled={fieldsDisabled || !canEdit}
                            onCheckedChange={setAllowCredentials}
                        />

                        <ToggleRow
                            id="cors-run-policies"
                            label="Run policies on preflight"
                            description="Enforce auth or rate-limiting on preflight requests."
                            hint={
                                <>
                                    By default preflight <span className="font-mono text-xs">OPTIONS</span> requests bypass policies. Enable
                                    to enforce auth or rate-limiting on preflights too.
                                </>
                            }
                            checked={runPolicies}
                            disabled={fieldsDisabled || !canEdit}
                            onCheckedChange={setRunPolicies}
                        />

                        <div className="flex items-center justify-between gap-4">
                            <div className="flex items-center gap-1.5">
                                <Label htmlFor="cors-max-age" className="text-sm">
                                    Max age (seconds)
                                </Label>
                                <InfoTooltip
                                    content={
                                        <>
                                            How long the browser may cache the preflight response. Use{' '}
                                            <span className="font-mono text-xs">-1</span> to disable caching.
                                        </>
                                    }
                                />
                            </div>
                            <Input
                                id="cors-max-age"
                                type="number"
                                min={-1}
                                value={maxAge}
                                disabled={fieldsDisabled || !canEdit}
                                onChange={e => setMaxAge(e.target.value)}
                                placeholder="e.g. 3600"
                                className="w-36"
                            />
                        </div>
                    </CardContent>
                </Card>
            </div>
        </TooltipProvider>
    );
}
