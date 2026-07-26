/*
 * Copyright (C) 2026 The Gravitee team (http://gravitee.io)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
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
    Button,
    Dialog,
    DialogContent,
    DialogDescription,
    DialogFooter,
    DialogHeader,
    DialogTitle,
    Field,
    FieldLabel,
    Input,
    RadioGroup,
    RadioGroupItem,
    Select,
    SelectContent,
    SelectItem,
    SelectTrigger,
    SelectValue,
} from '@gravitee/graphene-core';
import { useEffect, useMemo, useState } from 'react';

import type { ScopeOption } from '../hooks/useScopeCatalog';
import {
    PORTAL_GRANT_SCOPE_TYPE_LABELS,
    type ConsumeProvisioning,
    type PortalAccessLevel,
    type PortalGrantScopeType,
} from '../types/permissions.types';
import { loadPlanOptions, type PlanOption } from '../utils/plan-options';

const SCOPE_TYPE_ORDER: readonly PortalGrantScopeType[] = ['API', 'API_PRODUCT', 'AI_WORKSPACE', 'PORTAL'];

const SCOPE_TYPE_HINTS: Record<PortalGrantScopeType, string> = {
    API: 'A single API and its documentation pages',
    API_PRODUCT: 'A bundle of APIs published together',
    AI_WORKSPACE: 'A governed AI workspace and its models',
    PORTAL: 'Baseline access to a portal’s own pages, folders, and links',
};

export interface AddScopeValues {
    scopeType: PortalGrantScopeType;
    scopeId: string;
    access: PortalAccessLevel;
    provisioning?: ConsumeProvisioning;
    defaultPlanId?: string;
}

interface AddScopeDialogProps {
    readonly open: boolean;
    readonly groupName: string;
    readonly options: readonly ScopeOption[];
    readonly existingScopeKeys: ReadonlySet<string>;
    readonly onOpenChange: (open: boolean) => void;
    readonly onSubmit: (values: AddScopeValues) => void;
}

export function AddScopeDialog({
    open,
    groupName,
    options,
    existingScopeKeys,
    onOpenChange,
    onSubmit,
}: AddScopeDialogProps) {
    const [scopeType, setScopeType] = useState<PortalGrantScopeType>('API');
    const [scopeId, setScopeId] = useState('');
    const [query, setQuery] = useState('');
    const [access, setAccess] = useState<PortalAccessLevel>('VIEW');
    const [provisioning, setProvisioning] = useState<ConsumeProvisioning>('CLASSIC');
    const [defaultPlanId, setDefaultPlanId] = useState('');
    const [planOptions, setPlanOptions] = useState<PlanOption[]>([]);

    useEffect(() => {
        if (!open) {
            return;
        }

        setScopeType('API');
        setScopeId('');
        setQuery('');
        setAccess('VIEW');
        setProvisioning('CLASSIC');
        setDefaultPlanId('');
        setPlanOptions([]);
    }, [open]);

    // A portal grant is a visibility baseline, so consumption never applies to it.
    const canConsume = scopeType !== 'PORTAL';

    useEffect(() => {
        if (!canConsume) {
            setAccess('VIEW');
        }
    }, [canConsume]);

    useEffect(() => {
        if (!scopeId || access !== 'CONSUME') {
            setPlanOptions([]);
            return;
        }

        let cancelled = false;
        void loadPlanOptions(scopeType, scopeId).then(plans => {
            if (cancelled) {
                return;
            }
            setPlanOptions(plans);
            setDefaultPlanId(current => (plans.some(plan => plan.id === current) ? current : (plans[0]?.id ?? '')));
        });

        return () => {
            cancelled = true;
        };
    }, [access, scopeId, scopeType]);

    // AI workspaces are meant to skip the subscription workflow entirely.
    useEffect(() => {
        if (scopeType === 'AI_WORKSPACE') {
            setProvisioning('AUTO');
        }
    }, [scopeType]);

    const availableOptions = useMemo(() => {
        const normalized = query.trim().toLowerCase();
        return options
            .filter(option => option.scopeType === scopeType)
            .filter(option => !existingScopeKeys.has(`${option.scopeType}:${option.id}`))
            .filter(
                option =>
                    !normalized
                    || option.name.toLowerCase().includes(normalized)
                    || (option.description ?? '').toLowerCase().includes(normalized),
            );
    }, [existingScopeKeys, options, query, scopeType]);

    const selectedOption = availableOptions.find(option => option.id === scopeId);
    const canSubmit = scopeId.length > 0;

    const handleSubmit = () => {
        if (!canSubmit) {
            return;
        }

        onSubmit({
            scopeType,
            scopeId,
            access,
            provisioning: access === 'CONSUME' ? provisioning : undefined,
            defaultPlanId: access === 'CONSUME' && provisioning === 'AUTO' ? defaultPlanId : undefined,
        });
        onOpenChange(false);
    };

    return (
        <Dialog open={open} onOpenChange={onOpenChange}>
            <DialogContent
                className="flex flex-col gap-4 overflow-hidden"
                style={{
                    width: 'min(92vw, 38rem)',
                    maxWidth: 'min(92vw, 38rem)',
                    maxHeight: '90vh',
                }}
            >
                <DialogHeader className="shrink-0">
                    <DialogTitle>Add asset to {groupName}</DialogTitle>
                    <DialogDescription>
                        Grant access to an API, product, AI workspace, or portal. Navigation items inside it inherit
                        this access unless you override them.
                    </DialogDescription>
                </DialogHeader>

                <form
                    id="add-scope-form"
                    className="flex min-h-0 flex-1 flex-col gap-4 overflow-x-hidden overflow-y-auto py-1 pr-1"
                    onSubmit={event => {
                        event.preventDefault();
                        handleSubmit();
                    }}
                >
                    <Field className="shrink-0">
                        <FieldLabel htmlFor="scope-type">Asset type</FieldLabel>
                        <Select
                            value={scopeType}
                            onValueChange={value => {
                                setScopeType(value as PortalGrantScopeType);
                                setScopeId('');
                            }}
                        >
                            <SelectTrigger id="scope-type" className="w-full">
                                <SelectValue />
                            </SelectTrigger>
                            <SelectContent position="popper" align="start" className="z-50 w-[var(--radix-select-trigger-width)]">
                                {SCOPE_TYPE_ORDER.map(type => (
                                    <SelectItem key={type} value={type}>
                                        {PORTAL_GRANT_SCOPE_TYPE_LABELS[type]}
                                    </SelectItem>
                                ))}
                            </SelectContent>
                        </Select>
                        <p className="text-xs text-muted-foreground">{SCOPE_TYPE_HINTS[scopeType]}</p>
                    </Field>

                    <Field className="min-h-0 shrink">
                        <FieldLabel htmlFor="scope-search">
                            {PORTAL_GRANT_SCOPE_TYPE_LABELS[scopeType]}
                        </FieldLabel>
                        <Input
                            id="scope-search"
                            placeholder="Search…"
                            value={query}
                            onChange={event => setQuery(event.target.value)}
                        />
                        <div
                            className="space-y-1 rounded-md border p-1"
                            style={{ height: '12rem', overflowY: 'auto', overscrollBehavior: 'contain' }}
                        >
                            {availableOptions.length === 0 ? (
                                <p className="py-6 text-center text-sm text-muted-foreground">
                                    Nothing left to add for this asset type.
                                </p>
                            ) : (
                                availableOptions.map(option => (
                                    <label
                                        key={option.id}
                                        className={`flex cursor-pointer items-start gap-3 rounded-md px-2 py-2 text-sm ${
                                            option.id === scopeId ? 'bg-accent' : 'hover:bg-muted/50'
                                        }`}
                                    >
                                        <input
                                            type="radio"
                                            name="scope-id"
                                            className="mt-1 shrink-0"
                                            checked={option.id === scopeId}
                                            onChange={() => setScopeId(option.id)}
                                        />
                                        <span className="min-w-0 flex-1">
                                            <span className="block truncate font-medium">{option.name}</span>
                                            {option.description && (
                                                <span className="block truncate text-muted-foreground">
                                                    {option.description}
                                                </span>
                                            )}
                                        </span>
                                    </label>
                                ))
                            )}
                        </div>
                    </Field>

                    <Field className="shrink-0">
                        <FieldLabel>Access</FieldLabel>
                        <RadioGroup
                            value={access}
                            onValueChange={value => setAccess(value as PortalAccessLevel)}
                            className="gap-2"
                        >
                            <label className="flex cursor-pointer items-start gap-3 rounded-md border px-3 py-2 text-sm">
                                <RadioGroupItem value="VIEW" className="mt-0.5" />
                                <span>
                                    <span className="block font-medium">View</span>
                                    <span className="block text-muted-foreground">
                                        Members see the scope in the catalog, navigation, and documentation.
                                    </span>
                                </span>
                            </label>
                            <label
                                className={`flex items-start gap-3 rounded-md border px-3 py-2 text-sm ${
                                    canConsume ? 'cursor-pointer' : 'cursor-not-allowed opacity-60'
                                }`}
                            >
                                <RadioGroupItem value="CONSUME" className="mt-0.5" disabled={!canConsume} />
                                <span>
                                    <span className="block font-medium">Consume</span>
                                    <span className="block text-muted-foreground">
                                        {canConsume
                                            ? 'Members can also subscribe and obtain credentials.'
                                            : 'Portal scopes grant visibility only.'}
                                    </span>
                                </span>
                            </label>
                        </RadioGroup>
                    </Field>

                    {access === 'CONSUME' && (
                        <Field className="shrink-0">
                            <FieldLabel>Provisioning</FieldLabel>
                            <RadioGroup
                                value={provisioning}
                                onValueChange={value => setProvisioning(value as ConsumeProvisioning)}
                                className="gap-2"
                            >
                                <label className="flex cursor-pointer items-start gap-3 rounded-md border px-3 py-2 text-sm">
                                    <RadioGroupItem value="CLASSIC" className="mt-0.5" />
                                    <span>
                                        <span className="block font-medium">Classic subscription workflow</span>
                                        <span className="block text-muted-foreground">
                                            Members create an application, pick a plan, and request a
                                            subscription.
                                        </span>
                                    </span>
                                </label>
                                <label className="flex cursor-pointer items-start gap-3 rounded-md border px-3 py-2 text-sm">
                                    <RadioGroupItem value="AUTO" className="mt-0.5" />
                                    <span>
                                        <span className="block font-medium">Auto-provisioned</span>
                                        <span className="block text-muted-foreground">
                                            Each member’s default application is subscribed to the default
                                            plan; credentials are issued on first access.
                                        </span>
                                    </span>
                                </label>
                            </RadioGroup>
                        </Field>
                    )}

                    {access === 'CONSUME' && provisioning === 'AUTO' && (
                        <Field className="shrink-0">
                            <FieldLabel htmlFor="default-plan">Default plan</FieldLabel>
                            <Select
                                value={defaultPlanId}
                                onValueChange={setDefaultPlanId}
                                disabled={planOptions.length === 0}
                            >
                                <SelectTrigger id="default-plan" className="w-full">
                                    <SelectValue placeholder="Select a plan" />
                                </SelectTrigger>
                                <SelectContent position="popper" align="start" className="z-50 w-[var(--radix-select-trigger-width)]">
                                    {planOptions.map(plan => (
                                        <SelectItem key={plan.id} value={plan.id}>
                                            {plan.name}
                                        </SelectItem>
                                    ))}
                                </SelectContent>
                            </Select>
                            {planOptions.find(plan => plan.id === defaultPlanId)?.description && (
                                <p className="text-xs text-muted-foreground">
                                    {planOptions.find(plan => plan.id === defaultPlanId)?.description}
                                </p>
                            )}
                        </Field>
                    )}

                    {selectedOption && access === 'CONSUME' && provisioning === 'AUTO' && (
                        <Alert className="shrink-0">
                            <AlertDescription>
                                Members of {groupName} skip the subscription workflow for {selectedOption.name}.
                            </AlertDescription>
                        </Alert>
                    )}
                </form>

                <DialogFooter className="shrink-0 sm:justify-end">
                    <Button type="button" variant="outline" onClick={() => onOpenChange(false)}>
                        Cancel
                    </Button>
                    <Button type="submit" form="add-scope-form" disabled={!canSubmit}>
                        Add asset
                    </Button>
                </DialogFooter>
            </DialogContent>
        </Dialog>
    );
}
