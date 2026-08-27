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
import { Button, Input, Label, Switch } from '@gravitee/graphene-core';
import { useQuery } from '@tanstack/react-query';
import { useMemo, useState } from 'react';

import { TenantSelectInput } from './TenantSelectInput';
import { WizardStepIndicator } from '../../../../components/WizardStepIndicator';
import { getTenants } from '../../../../services/tenants';
import type { Tenant } from '../../../../types';
import { validateTcpPort } from '../../../../utils/apiCreationValidation';
import { validateDuplicateHost } from '../../../../utils/duplicateDialogValidation';
import { validateHttpProxyOptions } from '../../../../utils/endpointSharedConfiguration';
import type { HealthCheckConfigFormState, HealthCheckFormState } from '../../../../utils/healthCheckForm';
import { validateHealthCheckForm } from '../../../../utils/healthCheckForm';
import { tenantKeys } from '../../../../utils/queryKeys';
import { ConfigurationStep } from '../group-form/ConfigurationStep';
import { HealthCheckStep } from '../group-form/HealthCheckStep';
import type { EndpointFormState, SharedConfigFormState } from '../types';
import { DEFAULT_SHARED_CONFIG, newEndpointRow, validateEndpointName } from '../types';

const BASE_STEPS = [
    { id: 'general', label: 'General' },
    { id: 'configuration', label: 'Configuration' },
] as const;

const HEALTH_CHECK_STEP = { id: 'health-check', label: 'Health-check' } as const;

type BaseStepId = (typeof BASE_STEPS)[number]['id'];
type StepId = BaseStepId | 'health-check';

interface GeneralStepProps {
    form: EndpointFormState;
    existingNames: string[];
    tenantsLoading: boolean;
    availableTenants: Tenant[];
    isTcp?: boolean;
    onChange: <K extends keyof EndpointFormState>(key: K, value: EndpointFormState[K]) => void;
}

function EndpointGeneralStep({
    form,
    existingNames,
    tenantsLoading,
    availableTenants,
    isTcp = false,
    onChange,
}: Readonly<GeneralStepProps>) {
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
    const tcpHostError = validateDuplicateHost(form.tcpTargetHost);
    const tcpPortError = validateTcpPort(form.tcpTargetPort);
    const weightError = form.weight < 1 ? 'Weight must be at least 1.' : null;

    return (
        <div className="space-y-4">
            <div className="space-y-2">
                <Label htmlFor="ep-name" className="text-sm">
                    Name <span className="text-destructive">*</span>
                </Label>
                <Input id="ep-name" value={form.name} onChange={e => onChange('name', e.target.value)} placeholder="my-endpoint" />
                {nameError && <p className="text-xs text-destructive">{nameError}</p>}
                <p className="text-xs text-muted-foreground">Must be unique in this group. Colons are not allowed.</p>
            </div>

            {isTcp ? (
                <>
                    <div className="grid grid-cols-2 gap-4">
                        <div className="space-y-2">
                            <Label htmlFor="ep-tcp-host" className="text-sm">
                                Host <span className="text-destructive">*</span>
                            </Label>
                            <Input
                                id="ep-tcp-host"
                                value={form.tcpTargetHost}
                                onChange={e => onChange('tcpTargetHost', e.target.value)}
                                placeholder="postgres.internal.example.com"
                            />
                            {tcpHostError && <p className="text-xs text-destructive">{tcpHostError}</p>}
                        </div>
                        <div className="space-y-2">
                            <Label htmlFor="ep-tcp-port" className="text-sm">
                                Port <span className="text-destructive">*</span>
                            </Label>
                            <Input
                                id="ep-tcp-port"
                                inputMode="numeric"
                                value={form.tcpTargetPort}
                                onChange={e => onChange('tcpTargetPort', e.target.value)}
                                placeholder="5432"
                            />
                            {tcpPortError && <p className="text-xs text-destructive">{tcpPortError}</p>}
                        </div>
                    </div>
                    <div className="flex items-center justify-between rounded-lg border px-4 py-3">
                        <div>
                            <p className="text-sm font-medium">Secured (TLS)</p>
                            <p className="text-xs text-muted-foreground">Connect to the backend over TLS.</p>
                        </div>
                        <Switch
                            checked={form.tcpTargetSecured}
                            onCheckedChange={v => onChange('tcpTargetSecured', v)}
                            aria-label="Enable TLS to the backend"
                        />
                    </div>
                </>
            ) : (
                <div className="space-y-2">
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
            )}

            <div className="space-y-2">
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

            <div className="space-y-2">
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

interface EndpointFormProps {
    isEdit: boolean;
    initial?: EndpointFormState;
    existingNames: string[];
    showHealthCheck?: boolean;
    /** tcp-proxy endpoints have no shared-configuration or health-check step — just General (host/port/secured). */
    isTcp?: boolean;
    groupHealthCheck?: HealthCheckFormState;
    isReadOnly?: boolean;
    isSaving?: boolean;
    onSave: (ep: EndpointFormState) => void;
    onCancel: () => void;
}

export function EndpointForm({
    isEdit,
    initial,
    existingNames,
    showHealthCheck = false,
    isTcp = false,
    groupHealthCheck,
    isReadOnly = false,
    isSaving = false,
    onSave,
    onCancel,
}: Readonly<EndpointFormProps>) {
    const env = useEnvironment();
    const steps = useMemo(
        () => (isTcp ? [BASE_STEPS[0]] : showHealthCheck ? [...BASE_STEPS, HEALTH_CHECK_STEP] : [...BASE_STEPS]),
        [isTcp, showHealthCheck],
    );

    const [currentStep, setCurrentStep] = useState<StepId>('general');
    const [form, setForm] = useState<EndpointFormState>(initial ?? newEndpointRow(groupHealthCheck));
    const [configOverride, setConfigOverride] = useState<SharedConfigFormState>(initial?._configOverride ?? DEFAULT_SHARED_CONFIG);
    const [healthCheckErrors, setHealthCheckErrors] = useState<Record<string, string>>({});
    const [proxyError, setProxyError] = useState<string | null>(null);

    const { data: availableTenants = [], isLoading: tenantsLoading } = useQuery({
        queryKey: tenantKeys.list(env?.id ?? ''),
        queryFn: () => getTenants(env!.id),
        enabled: !!env?.id,
        staleTime: 5 * 60_000,
    });

    function setField<K extends keyof EndpointFormState>(key: K, value: EndpointFormState[K]) {
        setForm(prev => ({ ...prev, [key]: value }));
    }

    function patchHealthCheck(patch: Partial<HealthCheckFormState>) {
        setForm(prev => {
            const next = { ...prev.healthCheck, ...patch };
            if (patch.inherit === true && groupHealthCheck) {
                return {
                    ...prev,
                    healthCheck: {
                        enabled: groupHealthCheck.enabled,
                        inherit: true,
                        configuration: { ...groupHealthCheck.configuration },
                    },
                };
            }
            return { ...prev, healthCheck: next };
        });
        setHealthCheckErrors({});
    }

    function patchHealthCheckConfig(patch: Partial<HealthCheckConfigFormState>) {
        setForm(prev => ({
            ...prev,
            healthCheck: {
                ...prev.healthCheck,
                configuration: { ...prev.healthCheck.configuration, ...patch },
            },
        }));
        setHealthCheckErrors({});
    }

    const nameError = (() => {
        const base = validateEndpointName(form.name);
        if (base) return base;
        const lower = form.name.trim().toLowerCase();
        if (existingNames.some(n => n.toLowerCase() === lower)) return 'This name is already used by another endpoint in this group.';
        return null;
    })();

    const generalValid = isTcp
        ? !nameError &&
          validateDuplicateHost(form.tcpTargetHost) === null &&
          validateTcpPort(form.tcpTargetPort) === null &&
          form.weight >= 1
        : !nameError && form.target.trim().length > 0 && !/\s/.test(form.target) && form.weight >= 1;
    const configurationValid = isTcp || form.inheritConfiguration || validateHttpProxyOptions(configOverride.proxy) === null;
    const healthCheckValid = !showHealthCheck || Object.keys(validateHealthCheckForm(form.healthCheck)).length === 0;

    const currentStepIndex = steps.findIndex(s => s.id === currentStep);
    const isLastStep = currentStepIndex === steps.length - 1;
    const canGoBack = currentStepIndex > 0;

    function goNext() {
        if (currentStep === 'configuration' && !form.inheritConfiguration) {
            const err = validateHttpProxyOptions(configOverride.proxy);
            setProxyError(err);
            if (err) return;
        }
        if (currentStep === 'health-check') {
            const errors = validateHealthCheckForm(form.healthCheck);
            setHealthCheckErrors(errors);
            if (Object.keys(errors).length > 0) return;
        }
        const next = steps[currentStepIndex + 1];
        if (next) setCurrentStep(next.id);
    }

    function goBack() {
        const prev = steps[currentStepIndex - 1];
        if (prev) setCurrentStep(prev.id);
    }

    function handleSave() {
        if (!form.inheritConfiguration) {
            const proxyErr = validateHttpProxyOptions(configOverride.proxy);
            setProxyError(proxyErr);
            if (proxyErr) {
                setCurrentStep('configuration');
                return;
            }
        }
        if (showHealthCheck) {
            const errors = validateHealthCheckForm(form.healthCheck);
            setHealthCheckErrors(errors);
            if (Object.keys(errors).length > 0) {
                setCurrentStep('health-check');
                return;
            }
        }
        onSave({ ...form, _configOverride: configOverride });
    }

    const nextDisabled =
        (currentStep === 'general' && !generalValid) ||
        (currentStep === 'configuration' && !configurationValid) ||
        (currentStep === 'health-check' && !healthCheckValid);

    return (
        <div className="space-y-6">
            <WizardStepIndicator
                steps={steps}
                currentStepId={currentStep}
                onStepClick={id => setCurrentStep(id as StepId)}
                ariaLabel="Endpoint form steps"
            />

            <div>
                {currentStep === 'general' && (
                    <EndpointGeneralStep
                        form={form}
                        existingNames={existingNames}
                        tenantsLoading={tenantsLoading}
                        availableTenants={availableTenants}
                        isTcp={isTcp}
                        onChange={setField}
                    />
                )}
                {currentStep === 'configuration' && (
                    <ConfigurationStep
                        config={configOverride}
                        proxyError={proxyError}
                        onChange={patch => {
                            setConfigOverride(prev => ({ ...prev, ...patch }));
                            setProxyError(null);
                        }}
                    />
                )}
                {currentStep === 'health-check' && showHealthCheck && (
                    <HealthCheckStep
                        mode="endpoint"
                        healthCheck={form.healthCheck}
                        groupHealthCheck={groupHealthCheck}
                        errors={healthCheckErrors}
                        readOnly={isReadOnly}
                        onChange={patchHealthCheck}
                        onConfigChange={patchHealthCheckConfig}
                    />
                )}
            </div>

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
                        <Button type="button" size="sm" onClick={goNext} disabled={nextDisabled}>
                            Next
                        </Button>
                    ) : (
                        <Button
                            type="button"
                            size="sm"
                            onClick={handleSave}
                            disabled={!generalValid || !configurationValid || isSaving || (showHealthCheck && !healthCheckValid)}
                        >
                            {isSaving ? 'Saving…' : isEdit ? 'Save endpoint' : 'Add endpoint'}
                        </Button>
                    )}
                </div>
            </div>
        </div>
    );
}
