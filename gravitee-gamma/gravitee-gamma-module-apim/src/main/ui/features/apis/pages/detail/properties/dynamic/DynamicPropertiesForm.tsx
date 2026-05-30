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
import { Alert, AlertDescription, Button, Card, CardContent, Label, Switch } from '@gravitee/graphene-core';
import { useEffect, useRef, useState } from 'react';

import { HttpClientSection } from './sections/HttpClientSection';
import { HttpRequestSection } from './sections/HttpRequestSection';
import { ProxySection } from './sections/ProxySection';
import { SetupGuideBanner } from './sections/SetupGuideBanner';
import { SslSection } from './sections/SslSection';
import type { DynamicPropertiesFormState, FormErrors } from './types';
import { hasErrors, validateForm } from './types';
import { CronScheduleInput, validateCron } from '../../../../components/CronScheduleInput';

// ─── Export ───────────────────────────────────────────────────────────────────

interface DynamicPropertiesFormProps {
    /** Seeded from parent — render-phase diffing handles resets after invalidation. */
    initialState: DynamicPropertiesFormState;
    onSave: (state: DynamicPropertiesFormState) => void;
    isSaving: boolean;
    saveError?: string;
    isReadOnly: boolean;
}

export function DynamicPropertiesForm({ initialState, onSave, isSaving, saveError, isReadOnly }: Readonly<DynamicPropertiesFormProps>) {
    // ─── Form state ────────────────────────────────────────────────────────────
    const [form, setForm] = useState<DynamicPropertiesFormState>(initialState);
    const [submitted, setSubmitted] = useState(false);
    const [isDirty, setIsDirty] = useState(false);
    const savingRef = useRef(false);

    // Render-phase seeding: when the parent provides a new initialState reference (after
    // cache invalidation following a successful save) reset the form to match.
    const [prevInitial, setPrevInitial] = useState(initialState);
    if (prevInitial !== initialState) {
        setPrevInitial(initialState);
        setForm(initialState);
        setSubmitted(false);
        setIsDirty(false);
    }

    // Reset the double-submit guard once the mutation settles (success or error).
    useEffect(() => {
        if (!isSaving) savingRef.current = false;
    }, [isSaving]);

    function patch(p: Partial<DynamicPropertiesFormState>) {
        setForm(f => ({ ...f, ...p }));
        setIsDirty(true);
    }

    // ─── Validation ────────────────────────────────────────────────────────────
    const cronError = submitted ? (validateCron(form.schedule) ?? undefined) : undefined;
    const fieldErrors: FormErrors = submitted ? validateForm(form) : {};
    const invalid = submitted && (hasErrors(fieldErrors) || Boolean(cronError));

    // ─── Submit / Discard ──────────────────────────────────────────────────────
    function handleSave() {
        if (savingRef.current) return;
        setSubmitted(true);
        const errors = validateForm(form);
        const cronErr = validateCron(form.schedule);
        if (hasErrors(errors) || cronErr) return;
        savingRef.current = true;
        onSave(form);
    }

    function handleDiscard() {
        setForm(initialState);
        setSubmitted(false);
        setIsDirty(false);
        savingRef.current = false;
    }

    const fieldsDisabled = isReadOnly || isSaving;
    const configDisabled = !form.enabled || fieldsDisabled;

    return (
        <div className="space-y-4 pb-4">
            {/* ─── Save / Discard bar — pinned at the top, only when there are unsaved changes ─── */}
            {!isReadOnly && isDirty && (
                <div className="flex items-center justify-end gap-2 border-b pb-4">
                    <Button variant="outline" size="sm" onClick={handleDiscard} disabled={isSaving}>
                        Discard
                    </Button>
                    <Button size="sm" onClick={handleSave} disabled={isSaving}>
                        {isSaving ? 'Saving…' : 'Save changes'}
                    </Button>
                </div>
            )}

            {/* ─── Setup guide ────────────────────────────────────────────────── */}
            <SetupGuideBanner />

            {/* ─── Save error ──────────────────────────────────────────────────── */}
            {saveError && (
                <Alert variant="destructive">
                    <AlertDescription>{saveError}</AlertDescription>
                </Alert>
            )}

            {/* ─── Enable toggle ───────────────────────────────────────────────── */}
            <Card>
                <CardContent className="py-3 px-4">
                    <div className="flex items-start justify-between gap-4">
                        <div className="space-y-0.5">
                            <Label htmlFor="dp-enabled" className="text-sm font-medium">
                                Enable dynamic properties
                            </Label>
                            <p className="text-xs text-muted-foreground">
                                When enabled, the gateway polls the configured HTTP endpoint on the cron schedule and refreshes the property
                                store automatically.
                            </p>
                        </div>
                        <Switch
                            id="dp-enabled"
                            checked={form.enabled}
                            onCheckedChange={v => patch({ enabled: v })}
                            disabled={fieldsDisabled}
                        />
                    </div>
                </CardContent>
            </Card>

            {/* ─── Schedule ────────────────────────────────────────────────────── */}
            <Card>
                <CardContent className="pt-4 pb-3 px-4">
                    <CronScheduleInput
                        idPrefix="dp"
                        value={form.schedule}
                        error={cronError}
                        onChange={v => patch({ schedule: v })}
                        disabled={configDisabled}
                    />
                </CardContent>
            </Card>

            {/* ─── HTTP request ─────────────────────────────────────────────────── */}
            <Card>
                <CardContent className="pt-4 pb-3 px-4">
                    <HttpRequestSection
                        method={form.method}
                        url={form.url}
                        urlError={fieldErrors.url}
                        headers={form.headers}
                        body={form.body}
                        specification={form.specification}
                        useSystemProxy={form.useSystemProxy}
                        onChange={p => patch(p)}
                        disabled={configDisabled}
                    />
                </CardContent>
            </Card>

            {/* ─── Advanced collapsible sections ─────────────────────────────── */}
            <div className="space-y-3">
                <HttpClientSection
                    httpClient={form.httpClient}
                    onChange={p => patch({ httpClient: { ...form.httpClient, ...p } })}
                    disabled={configDisabled}
                />
                <ProxySection proxy={form.proxy} onChange={p => patch({ proxy: { ...form.proxy, ...p } })} disabled={configDisabled} />
                <SslSection ssl={form.ssl} onChange={p => patch({ ssl: { ...form.ssl, ...p } })} disabled={configDisabled} />
            </div>

            {/* ─── Validation summary ───────────────────────────────────────────── */}
            {invalid && (
                <Alert variant="destructive">
                    <AlertDescription>Please fix the errors above before saving.</AlertDescription>
                </Alert>
            )}
        </div>
    );
}
