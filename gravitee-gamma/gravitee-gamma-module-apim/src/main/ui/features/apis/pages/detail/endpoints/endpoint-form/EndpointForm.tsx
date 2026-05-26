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
import { Button, Input, Label } from '@gravitee/graphene-core';
import { useQuery } from '@tanstack/react-query';
import { useState } from 'react';

import { TenantSelectInput } from './TenantSelectInput';
import { getTenants } from '../../../../services/tenants';
import type { Tenant } from '../../../../types';
import { tenantKeys } from '../../../../utils/queryKeys';
import { ConfigurationStep } from '../group-form/ConfigurationStep';
import { HealthCheckStep } from '../group-form/HealthCheckStep';
import type { EndpointFormState, SharedConfigFormState } from '../types';
import { DEFAULT_SHARED_CONFIG, newEndpointRow, validateEndpointName } from '../types';
import { WizardStepIndicator } from '../WizardStepIndicator';

// ─── Steps ────────────────────────────────────────────────────────────────────

const STEPS = [
    { id: 'general', label: 'General' },
    { id: 'configuration', label: 'Configuration' },
    { id: 'health-check', label: 'Health-check', comingSoon: true },
] as const;

type StepId = (typeof STEPS)[number]['id'];

// ─── General step ─────────────────────────────────────────────────────────────

interface GeneralStepProps {
    form: EndpointFormState;
    existingNames: string[];
    tenantsLoading: boolean;
    availableTenants: Tenant[];
    onChange: <K extends keyof EndpointFormState>(key: K, value: EndpointFormState[K]) => void;
}

function EndpointGeneralStep({ form, existingNames, tenantsLoading, availableTenants, onChange }: Readonly<GeneralStepProps>) {
    const nameError = (() => {
        const base = validateEndpointName(form.name);
        if (base) return base;
        const lower = form.name.trim().toLowerCase();
        if (existingNames.some(n => n.toLowerCase() === lower)) return 'This name is already used by another endpoint in this group.';
        return null;
    })();

    const targetError = !form.target.trim()
        ? 'Target URL is required.'
        : /\s/.test(form.target)
          ? 'Target URL must not contain whitespace.'
          : null;
    const weightError = form.weight < 1 ? 'Weight must be at least 1.' : null;

    return (
        <div className="space-y-4">
            {/* Name */}
            <div className="space-y-1.5">
                <Label htmlFor="ep-name" className="text-sm">
                    Name <span className="text-destructive">*</span>
                </Label>
                <Input id="ep-name" value={form.name} onChange={e => onChange('name', e.target.value)} placeholder="my-endpoint" />
                {nameError && <p className="text-xs text-destructive">{nameError}</p>}
            </div>

            {/* Target URL */}
            <div className="space-y-1.5">
                <Label htmlFor="ep-target" className="text-sm">
                    Target URL <span className="text-destructive">*</span>
                </Label>
                <Input
                    id="ep-target"
                    value={form.target}
                    onChange={e => onChange('target', e.target.value)}
                    placeholder="https://backend.example.com"
                />
                {targetError && <p className="text-xs text-destructive">{targetError}</p>}
            </div>

            {/* Weight */}
            <div className="space-y-1.5">
                <Label htmlFor="ep-weight" className="text-sm">
                    Weight
                </Label>
                <Input
                    id="ep-weight"
                    type="number"
                    min={1}
                    value={form.weight}
                    onChange={e => {
                        const n = parseInt(e.target.value, 10);
                        if (!isNaN(n)) onChange('weight', n);
                    }}
                />
                {weightError && <p className="text-xs text-destructive">{weightError}</p>}
                <p className="text-xs text-muted-foreground">Used by weighted load balancers. Must be at least 1.</p>
            </div>

            {/* Tenants */}
            <div className="space-y-1.5">
                <Label className="text-sm">Tenants</Label>
                <p className="text-xs text-muted-foreground">Restrict this endpoint to requests from specific gateway tenants.</p>
                <TenantSelectInput
                    selectedKeys={form.tenants}
                    tenants={availableTenants}
                    isLoading={tenantsLoading}
                    onChange={keys => onChange('tenants', keys)}
                />
            </div>
        </div>
    );
}

// ─── Main component ───────────────────────────────────────────────────────────

interface EndpointFormProps {
    isEdit: boolean;
    initial?: EndpointFormState;
    existingNames: string[];
    isSaving?: boolean;
    onSave: (ep: EndpointFormState) => void;
    onCancel: () => void;
}

export function EndpointForm({ isEdit, initial, existingNames, isSaving = false, onSave, onCancel }: Readonly<EndpointFormProps>) {
    const env = useEnvironment();
    const [currentStep, setCurrentStep] = useState<StepId>('general');
    const [form, setForm] = useState<EndpointFormState>(initial ?? newEndpointRow());
    const [configOverride, setConfigOverride] = useState<SharedConfigFormState>(initial?._configOverride ?? DEFAULT_SHARED_CONFIG);

    const { data: availableTenants = [], isLoading: tenantsLoading } = useQuery({
        queryKey: tenantKeys.list(env?.id ?? ''),
        queryFn: () => getTenants(env!.id),
        enabled: !!env?.id,
        staleTime: 5 * 60 * 1000,
    });

    function set<K extends keyof EndpointFormState>(key: K, value: EndpointFormState[K]) {
        setForm(prev => ({ ...prev, [key]: value }));
    }

    const nameError = (() => {
        const base = validateEndpointName(form.name);
        if (base) return base;
        const lower = form.name.trim().toLowerCase();
        if (existingNames.some(n => n.toLowerCase() === lower)) return 'This name is already used by another endpoint in this group.';
        return null;
    })();

    const generalValid = !nameError && form.target.trim().length > 0 && !/\s/.test(form.target) && form.weight >= 1;

    const currentStepIndex = STEPS.findIndex(s => s.id === currentStep);
    const isLastStep = currentStep === 'health-check';
    const canGoBack = currentStepIndex > 0;

    function goNext() {
        const next = STEPS[currentStepIndex + 1];
        if (next) setCurrentStep(next.id);
    }

    function goBack() {
        const prev = STEPS[currentStepIndex - 1];
        if (prev) setCurrentStep(prev.id);
    }

    function handleSave() {
        onSave({ ...form, _configOverride: configOverride });
    }

    return (
        <div className="space-y-6">
            {/* ── Stepper ── */}
            <WizardStepIndicator
                steps={STEPS}
                currentStepId={currentStep}
                onStepClick={id => setCurrentStep(id as StepId)}
                ariaLabel="Endpoint form steps"
            />

            {/* ── Step content ── */}
            <div>
                {currentStep === 'general' && (
                    <EndpointGeneralStep
                        form={form}
                        existingNames={existingNames}
                        tenantsLoading={tenantsLoading}
                        availableTenants={availableTenants}
                        onChange={set}
                    />
                )}
                {currentStep === 'configuration' && (
                    <ConfigurationStep config={configOverride} onChange={patch => setConfigOverride(prev => ({ ...prev, ...patch }))} />
                )}
                {currentStep === 'health-check' && <HealthCheckStep />}
            </div>

            {/* ── Navigation ── */}
            <div className="flex items-center justify-between">
                <div className="flex items-center gap-2">
                    {canGoBack && (
                        <Button type="button" size="sm" variant="outline" onClick={goBack}>
                            Back
                        </Button>
                    )}
                    <Button type="button" size="sm" variant="outline" onClick={onCancel}>
                        Cancel
                    </Button>
                </div>

                <div className="flex items-center gap-2">
                    {!isLastStep ? (
                        <Button type="button" size="sm" onClick={goNext} disabled={currentStep === 'general' && !generalValid}>
                            Next
                        </Button>
                    ) : (
                        <Button type="button" size="sm" onClick={handleSave} disabled={!generalValid || isSaving}>
                            {isSaving ? 'Saving…' : isEdit ? 'Save endpoint' : 'Add endpoint'}
                        </Button>
                    )}
                </div>
            </div>
        </div>
    );
}
