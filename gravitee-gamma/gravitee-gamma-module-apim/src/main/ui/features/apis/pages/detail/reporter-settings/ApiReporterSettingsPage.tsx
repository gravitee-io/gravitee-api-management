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
    Skeleton,
    Switch,
} from '@gravitee/graphene-core';
import { ActivityIcon, TriangleAlertIcon } from '@gravitee/graphene-core/icons';
import { useMutation, useQueryClient } from '@tanstack/react-query';
import { useCallback, useEffect, useRef, useState } from 'react';
import { useParams } from 'react-router-dom';

import { SpanRedactionRules } from './SpanRedactionRules';
import { notify } from '../../../../../shared/notify';
import { useApiDetail } from '../../../hooks/useApiDetail';
import { updateApiAnalytics } from '../../../services/apis';
import type { Analytics, RedactionRule } from '../../../types';
import { apiDetailKeys } from '../../../utils/queryKeys';

// ─── Row ──────────────────────────────────────────────────────────────────────

function SettingRow({
    id,
    label,
    description,
    checked,
    disabled,
    onCheckedChange,
}: {
    id: string;
    label: string;
    description?: string;
    checked: boolean;
    disabled?: boolean;
    onCheckedChange: (v: boolean) => void;
}) {
    return (
        <div className="flex items-start justify-between gap-4">
            <div className="min-w-0">
                <Label htmlFor={id} className="text-sm font-medium">
                    {label}
                </Label>
                {description && <p className="text-xs text-muted-foreground mt-0.5 max-w-xl">{description}</p>}
            </div>
            <Switch id={id} checked={checked} onCheckedChange={onCheckedChange} disabled={disabled} />
        </div>
    );
}

// ─── Page ─────────────────────────────────────────────────────────────────────

export function ApiReporterSettingsPage() {
    const { apiId } = useParams<{ apiId: string }>();
    const env = useEnvironment();
    const queryClient = useQueryClient();

    const canEdit = useHasPermission({ anyOf: ['api-definition-u'] });
    const { data: api, isLoading, isError } = useApiDetail(apiId);

    // ─── Form state ───────────────────────────────────────────────────────────
    const [enabled, setEnabled] = useState(false);
    const [entrypoint, setEntrypoint] = useState(false);
    const [endpoint, setEndpoint] = useState(false);
    const [request, setRequest] = useState(false);
    const [response, setResponse] = useState(false);
    const [headers, setHeaders] = useState(false);
    const [payload, setPayload] = useState(false);
    const [condition, setCondition] = useState('');
    const [tracingEnabled, setTracingEnabled] = useState(false);
    const [tracingVerbose, setTracingVerbose] = useState(false);
    const [otelLogsEnabled, setOtelLogsEnabled] = useState(false);
    const [redactionRules, setRedactionRules] = useState<RedactionRule[]>([]);
    const [isDirty, setIsDirty] = useState(false);

    const applyAnalyticsToForm = useCallback((analytics: Analytics | undefined) => {
        setEnabled(analytics?.enabled ?? false);
        setEntrypoint(analytics?.logging?.mode?.entrypoint ?? false);
        setEndpoint(analytics?.logging?.mode?.endpoint ?? false);
        setRequest(analytics?.logging?.phase?.request ?? false);
        setResponse(analytics?.logging?.phase?.response ?? false);
        setHeaders(analytics?.logging?.content?.headers ?? false);
        setPayload(analytics?.logging?.content?.payload ?? false);
        setCondition(analytics?.logging?.condition ?? '');
        setTracingEnabled(analytics?.tracing?.enabled ?? false);
        setTracingVerbose(analytics?.tracing?.verbose ?? false);
        setOtelLogsEnabled(analytics?.otelLogs?.enabled ?? false);
        setRedactionRules(analytics?.tracing?.redaction?.rules ?? []);
        setIsDirty(false);
    }, []);

    function markDirty() {
        setIsDirty(true);
    }

    // Initialize form once per apiId — prevents background refetches from wiping in-progress edits
    const initializedForApiIdRef = useRef<string | undefined>(undefined);

    useEffect(() => {
        if (!api || initializedForApiIdRef.current === apiId) return;
        initializedForApiIdRef.current = apiId;
        applyAnalyticsToForm(api.analytics);
    }, [api, apiId, applyAnalyticsToForm]);

    function handleDiscard() {
        applyAnalyticsToForm(api?.analytics);
    }

    // ─── Derived disabled states ───────────────────────────────────────────────
    const loggingFieldsDisabled = !enabled || (!entrypoint && !endpoint);
    const tracingVerboseDisabled = !enabled || !tracingEnabled;
    const otelLogsDisabled = !enabled || !tracingEnabled;

    // ─── Mutation ─────────────────────────────────────────────────────────────
    const mutation = useMutation({
        mutationFn: (analytics: Analytics) => updateApiAnalytics(env!.id, apiId!, analytics),
        onSuccess: () => {
            void queryClient.invalidateQueries({ queryKey: apiDetailKeys.detail(env?.id ?? '', apiId ?? '') });
            initializedForApiIdRef.current = undefined;
            setIsDirty(false);
            notify.success('Reporter settings saved');
        },
        onError: error => notify.error(error, 'Failed to save reporter settings.'),
    });

    const handleSave = () => {
        if (!api) return;
        const effectiveTracingEnabled = enabled && tracingEnabled;
        const effectiveTracingVerbose = effectiveTracingEnabled && tracingVerbose;
        mutation.mutate({
            enabled,
            logging: {
                mode: { entrypoint, endpoint },
                phase: { request, response },
                content: { headers, payload },
                ...(condition.trim() ? { condition: condition.trim() } : {}),
            },
            tracing: {
                ...(api.analytics?.tracing ?? {}),
                enabled: effectiveTracingEnabled,
                verbose: effectiveTracingVerbose,
                redaction: {
                    ...(api.analytics?.tracing?.redaction ?? {}),
                    rules: redactionRules,
                },
            },
            otelLogs: { enabled: otelLogsEnabled },
            sampling: api.analytics?.sampling,
        });
    };

    // ─── Loading ──────────────────────────────────────────────────────────────
    if (isLoading) {
        return (
            <div className="space-y-6">
                <div className="flex items-start justify-between gap-4">
                    <div className="space-y-2">
                        <Skeleton className="h-8 w-48 rounded" />
                        <Skeleton className="h-4 w-80 rounded" />
                    </div>
                    <Skeleton className="h-8 w-28 rounded" />
                </div>
                <Skeleton className="h-14 w-full rounded-lg" />
                <Skeleton className="h-64 w-full rounded-lg" />
                <Skeleton className="h-40 w-full rounded-lg" />
            </div>
        );
    }

    if (isError) {
        return (
            <Card className="border-destructive/30">
                <CardContent className="pt-4 pb-4">
                    <p className="text-sm text-destructive">Failed to load reporter settings. Please try again.</p>
                </CardContent>
            </Card>
        );
    }

    return (
        <div className="space-y-6">
            {/* ─── Header ─────────────────────────────────────────────────── */}
            <div className="flex items-start justify-between gap-4">
                <div className="space-y-1">
                    <h1 className="text-2xl font-semibold">Reporter Settings</h1>
                    <p className="text-sm text-muted-foreground">
                        Control how request and response data is reported for logs and analytics pipelines.
                    </p>
                </div>
                {canEdit && isDirty && (
                    <div className="flex items-center gap-2 shrink-0">
                        <Button size="sm" variant="outline" onClick={handleDiscard} aria-label="Discard changes">
                            Discard
                        </Button>
                        <Button size="sm" onClick={handleSave} disabled={mutation.isPending}>
                            {mutation.isPending ? 'Saving…' : 'Save changes'}
                        </Button>
                    </div>
                )}
            </div>

            {/* ─── Info banner ─────────────────────────────────────────────── */}
            <Alert>
                <AlertDescription>
                    <span className="font-semibold">Logging smartly — </span>
                    Enabling detailed logging increases storage and can affect gateway performance. Use payload logging and verbose tracing
                    only when needed.
                </AlertDescription>
            </Alert>

            {/* ─── Settings card ───────────────────────────────────────────── */}
            <Card>
                <CardHeader>
                    <div className="flex items-start justify-between gap-4">
                        <div>
                            <CardTitle className="text-base">Settings</CardTitle>
                            <p className="text-sm text-muted-foreground mt-0.5">Adjust reporter behavior for this API proxy.</p>
                        </div>
                        <Switch
                            id="reporter-enabled"
                            checked={enabled}
                            onCheckedChange={v => {
                                setEnabled(v);
                                markDirty();
                            }}
                            disabled={!canEdit}
                            aria-label="Enable analytics"
                        />
                    </div>
                </CardHeader>
                <CardContent className="space-y-0 divide-y divide-border">
                    {/* Logging mode */}
                    <div className="space-y-4 pb-6">
                        <div>
                            <p className="text-sm font-medium">Logging mode</p>
                            <p className="text-xs text-muted-foreground mt-0.5">
                                Choose which connection leg to include in reported events.
                            </p>
                        </div>
                        <SettingRow
                            id="reporter-entrypoint"
                            label="Entrypoint"
                            description="Client → gateway"
                            checked={entrypoint}
                            disabled={!enabled || !canEdit}
                            onCheckedChange={v => {
                                setEntrypoint(v);
                                markDirty();
                            }}
                        />
                        <SettingRow
                            id="reporter-endpoint"
                            label="Endpoint"
                            description="Gateway → upstream"
                            checked={endpoint}
                            disabled={!enabled || !canEdit}
                            onCheckedChange={v => {
                                setEndpoint(v);
                                markDirty();
                            }}
                        />
                    </div>

                    {/* Logging phase */}
                    <div className="space-y-4 py-6">
                        <div>
                            <p className="text-sm font-medium">Logging phase</p>
                            <p className="text-xs text-muted-foreground mt-0.5">Which lifecycle phases to capture.</p>
                        </div>
                        <SettingRow
                            id="reporter-request"
                            label="Request"
                            checked={request}
                            disabled={loggingFieldsDisabled || !canEdit}
                            onCheckedChange={v => {
                                setRequest(v);
                                markDirty();
                            }}
                        />
                        <SettingRow
                            id="reporter-response"
                            label="Response"
                            checked={response}
                            disabled={loggingFieldsDisabled || !canEdit}
                            onCheckedChange={v => {
                                setResponse(v);
                                markDirty();
                            }}
                        />
                    </div>

                    {/* Content data */}
                    <div className="space-y-4 py-6">
                        <div>
                            <p className="text-sm font-medium">Content data</p>
                            <p className="text-xs text-muted-foreground mt-0.5">
                                Optional inclusion of headers and bodies in log payloads.
                            </p>
                        </div>
                        <SettingRow
                            id="reporter-headers"
                            label="Headers"
                            checked={headers}
                            disabled={loggingFieldsDisabled || !canEdit}
                            onCheckedChange={v => {
                                setHeaders(v);
                                markDirty();
                            }}
                        />
                        <SettingRow
                            id="reporter-payload"
                            label="Payload"
                            checked={payload}
                            disabled={loggingFieldsDisabled || !canEdit}
                            onCheckedChange={v => {
                                setPayload(v);
                                markDirty();
                            }}
                        />
                    </div>

                    {/* Display conditions */}
                    <div className="space-y-3 pt-6">
                        <div>
                            <p className="text-sm font-medium">Display conditions</p>
                            <p className="text-xs text-muted-foreground mt-0.5">
                                Expression Language filter for the request phase (optional).
                            </p>
                        </div>
                        <div className="space-y-1.5">
                            <Label htmlFor="reporter-condition" className="text-xs text-muted-foreground">
                                Request phase condition
                            </Label>
                            <Input
                                id="reporter-condition"
                                value={condition}
                                onChange={e => {
                                    setCondition(e.target.value);
                                    markDirty();
                                }}
                                disabled={loggingFieldsDisabled || !canEdit}
                                placeholder="{#request.headers['Content-Type'][0] == 'application/json'}"
                            />
                            <p className="text-xs text-muted-foreground">Supports EL expressions. Leave empty to log all requests.</p>
                        </div>
                    </div>
                </CardContent>
            </Card>

            {/* ─── OpenTelemetry card ──────────────────────────────────────── */}
            <Card>
                <CardHeader>
                    <CardTitle className="flex items-center gap-2 text-base">
                        <ActivityIcon className="size-4 text-primary" />
                        OpenTelemetry
                    </CardTitle>
                    <p className="text-sm text-muted-foreground mt-0.5">Configure distributed tracing and OTel log correlation.</p>
                </CardHeader>
                <CardContent className="space-y-4">
                    <SettingRow
                        id="reporter-tracing-enabled"
                        label="Trace enabled"
                        description="Enable OpenTelemetry tracing for this API. Captures execution spans and conditions."
                        checked={tracingEnabled}
                        disabled={!enabled || !canEdit}
                        onCheckedChange={v => {
                            setTracingEnabled(v);
                            markDirty();
                        }}
                    />
                    <SettingRow
                        id="reporter-tracing-verbose"
                        label="Verbose"
                        description="Adds detailed span events with headers, context attributes, and policy execution details. Enable only for deep debugging — increases trace size significantly."
                        checked={tracingVerbose}
                        disabled={tracingVerboseDisabled || !canEdit}
                        onCheckedChange={v => {
                            setTracingVerbose(v);
                            markDirty();
                        }}
                    />
                    {tracingEnabled && tracingVerbose && (
                        <Alert variant="destructive">
                            <TriangleAlertIcon className="size-4" />
                            <AlertDescription>
                                Verbose mode significantly increases trace size. Disable after debugging is complete.
                            </AlertDescription>
                        </Alert>
                    )}
                    <SettingRow
                        id="reporter-otel-logs"
                        label="OTel Logs"
                        description="Emit request and response payloads as OTel log records correlated to the active trace. Enables log-to-trace linking in Grafana and other OTel-compatible backends."
                        checked={otelLogsEnabled}
                        disabled={otelLogsDisabled || !canEdit}
                        onCheckedChange={v => {
                            setOtelLogsEnabled(v);
                            markDirty();
                        }}
                    />

                    {tracingEnabled && tracingVerbose && (
                        <div className="pt-2 border-t border-border">
                            <SpanRedactionRules
                                key={apiId}
                                rules={redactionRules}
                                disabled={!canEdit}
                                onChange={rules => {
                                    setRedactionRules(rules);
                                    markDirty();
                                }}
                            />
                        </div>
                    )}
                </CardContent>
            </Card>
        </div>
    );
}
