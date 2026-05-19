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
import {
    Alert,
    AlertDescription,
    Button,
    Card,
    CardContent,
    CardHeader,
    CardTitle,
    Input,
    Label,
    Separator,
    Skeleton,
    TooltipProvider,
} from '@gravitee/graphene-core';
import { useMutation, useQueryClient } from '@tanstack/react-query';
import { useEffect, useRef, useState } from 'react';
import { useParams } from 'react-router-dom';

import { InfoTooltip } from '../features/apis/components/detail/cors/InfoTooltip';
import { CircuitBreakerDialog } from '../features/apis/components/detail/failover/CircuitBreakerDialog';
import { NumberField } from '../features/apis/components/detail/failover/NumberField';
import { ToggleSetting } from '../features/apis/components/detail/failover/ToggleSetting';
import { useApiDetailContext } from '../features/apis/context/ApiDetailContext';
import type { Failover } from '../features/apis/types/api';
import { apiDetailKeys } from '../features/apis/utils/queryKeys';
import { updateApiFailover } from '../services/apis/apis';

// ─── Defaults (match backend Failover.java constants) ────────────────────────

const DEFAULTS: Required<Omit<Failover, 'failureCondition'>> & { failureCondition: string } = {
    enabled: false,
    maxRetries: 2,
    slowCallDuration: 2000,
    openStateDuration: 10000,
    maxFailures: 5,
    perSubscription: true,
    failureCondition: '',
    forceNextEndpointOnFailure: false,
};

// ─── Page ─────────────────────────────────────────────────────────────────────

export function ApiFailoverPage() {
    const { apiId } = useParams<{ apiId: string }>();
    const env = useEnvironment();
    const queryClient = useQueryClient();

    const { api, isLoading, permissionsReady } = useApiDetailContext();

    // Requires both api-definition-u AND api-gateway_definition-u (mirrors legacy entrypoints guard).
    // Kubernetes-managed APIs are always read-only regardless of user permissions.
    const canEdit = useHasPermission({ allOf: ['api-definition-u', 'api-gateway_definition-u'] });
    const isKubernetesManaged = api?.definitionContext?.origin === 'KUBERNETES';
    const isReadOnly = !permissionsReady || !canEdit || isKubernetesManaged;

    // ─── Form state ───────────────────────────────────────────────────────────
    const [enabled, setEnabled] = useState(DEFAULTS.enabled);
    const [maxRetries, setMaxRetries] = useState(DEFAULTS.maxRetries);
    const [failureCondition, setFailureCondition] = useState(DEFAULTS.failureCondition);
    const [forceNextEndpoint, setForceNextEndpoint] = useState(DEFAULTS.forceNextEndpointOnFailure);
    const [slowCallDuration, setSlowCallDuration] = useState(DEFAULTS.slowCallDuration);
    const [openStateDuration, setOpenStateDuration] = useState(DEFAULTS.openStateDuration);
    const [maxFailures, setMaxFailures] = useState(DEFAULTS.maxFailures);
    const [perSubscription, setPerSubscription] = useState(DEFAULTS.perSubscription);

    // Initialize once per apiId — prevents background refetches from wiping in-progress edits
    const initializedForApiIdRef = useRef<string | undefined>(undefined);

    useEffect(() => {
        const saved = api?.failover;
        if (!saved || initializedForApiIdRef.current === apiId) return;
        initializedForApiIdRef.current = apiId;
        setEnabled(saved.enabled ?? DEFAULTS.enabled);
        setMaxRetries(saved.maxRetries ?? DEFAULTS.maxRetries);
        setFailureCondition(saved.failureCondition ?? DEFAULTS.failureCondition);
        setForceNextEndpoint(saved.forceNextEndpointOnFailure ?? DEFAULTS.forceNextEndpointOnFailure);
        setSlowCallDuration(saved.slowCallDuration ?? DEFAULTS.slowCallDuration);
        setOpenStateDuration(saved.openStateDuration ?? DEFAULTS.openStateDuration);
        setMaxFailures(saved.maxFailures ?? DEFAULTS.maxFailures);
        setPerSubscription(saved.perSubscription ?? DEFAULTS.perSubscription);
    }, [api?.failover, apiId]);

    // ─── Mutation ─────────────────────────────────────────────────────────────
    const mutation = useMutation({
        mutationFn: (failover: Failover) => updateApiFailover(env!.id, apiId!, failover),
        onSuccess: () => {
            void queryClient.invalidateQueries({ queryKey: apiDetailKeys.detail(env?.id ?? '', apiId ?? '') });
        },
    });

    const handleSave = () => {
        mutation.mutate({
            enabled,
            maxRetries,
            failureCondition: failureCondition.trim() || undefined,
            forceNextEndpointOnFailure: forceNextEndpoint,
            slowCallDuration,
            openStateDuration,
            maxFailures,
            perSubscription,
        });
    };

    const [dialogOpen, setDialogOpen] = useState(false);

    const fieldsDisabled = !enabled || isReadOnly;

    // ─── Loading ──────────────────────────────────────────────────────────────
    if (isLoading) {
        return (
            <div className="space-y-6 p-6">
                <div className="flex items-start justify-between gap-4">
                    <div className="space-y-2">
                        <Skeleton className="h-8 w-24 rounded" />
                        <Skeleton className="h-4 w-72 rounded" />
                    </div>
                    <Skeleton className="h-8 w-28 rounded" />
                </div>
                <Skeleton className="h-20 w-full rounded-lg" />
                <Skeleton className="h-48 w-full rounded-lg" />
                <Skeleton className="h-48 w-full rounded-lg" />
            </div>
        );
    }

    return (
        <TooltipProvider>
            <div className="space-y-6 p-6">
                {/* ─── Header ─────────────────────────────────────────────────── */}
                <div className="flex items-start justify-between gap-4">
                    <div className="space-y-1">
                        <h1 className="text-2xl font-semibold">Failover</h1>
                        <p className="text-sm text-muted-foreground">
                            Configure automatic retries and circuit breaker behavior for endpoint failover.
                        </p>
                    </div>
                    {!isReadOnly && (
                        <Button size="sm" onClick={handleSave} disabled={mutation.isPending}>
                            {mutation.isPending ? 'Saving…' : 'Save changes'}
                        </Button>
                    )}
                </div>

                {/* ─── Kubernetes read-only banner ─────────────────────────────── */}
                {isKubernetesManaged && (
                    <Alert>
                        <AlertDescription>
                            This API is managed by the Kubernetes operator. Failover settings are read-only.
                        </AlertDescription>
                    </Alert>
                )}

                {/* ─── Mutation error ──────────────────────────────────────────── */}
                {mutation.isError && (
                    <Alert variant="destructive">
                        <AlertDescription>{mutation.error?.message ?? 'Failed to save failover settings.'}</AlertDescription>
                    </Alert>
                )}

                {/* ─── Enable failover ─────────────────────────────────────────── */}
                <Card>
                    <CardContent className="space-y-4 pt-6">
                        <ToggleSetting
                            id="failover-enabled"
                            label="Enable failover"
                            description="Turn on automatic retries with a circuit breaker."
                            checked={enabled}
                            disabled={isReadOnly}
                            onCheckedChange={setEnabled}
                        />
                        <Alert>
                            <AlertDescription>
                                <div className="flex items-start justify-between gap-4">
                                    <p className="text-sm">
                                        Failover mechanism operates using a circuit breaker system. When a certain threshold of slow calls
                                        or connection failures (ssl error, unknown host, ...) is reached, the circuit breaker reaches
                                        &ldquo;open&rdquo; state, stopping all requests to the backend. During this time, the API will
                                        answer with a 502 - Bad Gateway status.
                                    </p>
                                    <Button
                                        type="button"
                                        variant="outline"
                                        size="sm"
                                        className="shrink-0"
                                        onClick={() => setDialogOpen(true)}
                                    >
                                        More information
                                    </Button>
                                </div>
                            </AlertDescription>
                        </Alert>
                    </CardContent>
                </Card>

                {/* ─── Retry policy ────────────────────────────────────────────── */}
                <Card>
                    <CardHeader>
                        <CardTitle className="flex items-center gap-1.5 text-base">
                            Retry policy
                            <InfoTooltip content="How aggressively the gateway retries before giving up." />
                        </CardTitle>
                    </CardHeader>
                    <CardContent className="space-y-6">
                        <div className="grid grid-cols-2 gap-6">
                            <NumberField
                                id="failover-max-retries"
                                label="Max retries"
                                value={maxRetries}
                                min={0}
                                disabled={fieldsDisabled}
                                onChange={setMaxRetries}
                            />
                            <div className="space-y-2">
                                <Label htmlFor="failover-failure-condition" className="flex items-center gap-1.5 text-sm">
                                    Failure condition
                                    <InfoTooltip content="EL expression evaluated against each response — return true to mark it as a failure." />
                                </Label>
                                <Input
                                    id="failover-failure-condition"
                                    value={failureCondition}
                                    disabled={fieldsDisabled}
                                    onChange={e => setFailureCondition(e.target.value)}
                                    placeholder="{#response.status >= 500}"
                                    className="font-mono text-sm"
                                />
                                <p className="text-xs text-muted-foreground">
                                    EL expression evaluated on the response. Leave empty to use connection errors only.
                                </p>
                            </div>
                        </div>

                        <Separator />

                        <ToggleSetting
                            id="failover-force-next-endpoint"
                            label="Force next endpoint on failure"
                            description="Skip the load balancer and pin retries to a different endpoint."
                            checked={forceNextEndpoint}
                            disabled={fieldsDisabled}
                            onCheckedChange={setForceNextEndpoint}
                        />
                    </CardContent>
                </Card>

                {/* ─── Circuit breaker ─────────────────────────────────────────── */}
                <Card>
                    <CardHeader>
                        <CardTitle className="flex items-center gap-1.5 text-base">
                            Circuit breaker
                            <InfoTooltip content="Conditions for opening the circuit and how long it stays open before transitioning to half-open." />
                        </CardTitle>
                    </CardHeader>
                    <CardContent className="space-y-6">
                        <div className="grid grid-cols-2 gap-6">
                            <div className="space-y-2">
                                <Label htmlFor="failover-slow-call-duration" className="text-sm">
                                    Slow call duration (ms)
                                </Label>
                                <Input
                                    id="failover-slow-call-duration"
                                    type="number"
                                    min={50}
                                    value={slowCallDuration}
                                    disabled={fieldsDisabled}
                                    onChange={e => {
                                        const n = parseInt(e.target.value, 10);
                                        if (!isNaN(n)) setSlowCallDuration(n);
                                    }}
                                />
                                <Alert>
                                    <AlertDescription className="text-xs">
                                        Endpoints should be configured with timeouts greater than the slow call duration.
                                    </AlertDescription>
                                </Alert>
                            </div>
                            <div className="space-y-2">
                                <Label htmlFor="failover-open-state-duration" className="text-sm">
                                    Open state duration (ms)
                                </Label>
                                <Input
                                    id="failover-open-state-duration"
                                    type="number"
                                    min={500}
                                    value={openStateDuration}
                                    disabled={fieldsDisabled}
                                    onChange={e => {
                                        const n = parseInt(e.target.value, 10);
                                        if (!isNaN(n)) setOpenStateDuration(n);
                                    }}
                                />
                                <Alert>
                                    <AlertDescription className="text-xs">
                                        In the half-open state, there is no retry mechanism. Transitioning to this state opens the circuit
                                        breaker if the next call is slow, otherwise it will close it.
                                    </AlertDescription>
                                </Alert>
                            </div>
                        </div>

                        <div className="grid grid-cols-2 gap-6 items-start">
                            <NumberField
                                id="failover-max-failures"
                                label="Maximum failures"
                                value={maxFailures}
                                min={1}
                                disabled={fieldsDisabled}
                                onChange={setMaxFailures}
                            />
                            <div className="pt-7">
                                <ToggleSetting
                                    id="failover-per-subscription"
                                    label="Per subscription circuit breaker"
                                    description="A noisy subscriber will not affect other consumers."
                                    checked={perSubscription}
                                    disabled={fieldsDisabled}
                                    onCheckedChange={setPerSubscription}
                                />
                            </div>
                        </div>
                    </CardContent>
                </Card>
            </div>

            <CircuitBreakerDialog open={dialogOpen} onOpenChange={setDialogOpen} />
        </TooltipProvider>
    );
}
