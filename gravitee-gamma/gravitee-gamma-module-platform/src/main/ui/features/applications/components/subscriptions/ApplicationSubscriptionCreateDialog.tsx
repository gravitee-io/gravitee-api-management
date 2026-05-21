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
    Badge,
    Button,
    cn,
    Dialog,
    DialogContent,
    DialogDescription,
    DialogFooter,
    DialogHeader,
    DialogTitle,
    Input,
    Label,
    Textarea,
} from '@gravitee/graphene-core';
import { SearchIcon } from '@gravitee/graphene-core/icons';
import { useEffect, useMemo, useState } from 'react';
import { useNavigate } from 'react-router-dom';

import {
    useApplicationApiKeySubscriptions,
    useCreateApplicationSubscription,
    useSubscribablePlans,
    useSubscriptionReferenceSearch,
} from '../../hooks/useCreateApplicationSubscription';
import { useEnvironmentPortalConfiguration } from '../../hooks/useEnvironmentPortalConfiguration';
import { isSharedApiKeyEnabled } from '../../services/environmentPortal';
import type { ApiKeyMode, ApplicationListItem } from '../../types/application';
import type { SubscribablePlan, SubscriptionReferenceSelection, SubscriptionSearchResultItem } from '../../types/applicationSubscription';
import {
    buildSubscriptionCreatePayload,
    getSubscriptionCreateValidationError,
    isPlanDisabled,
    planDisabledReason,
    shouldShowApiKeyModeChoice,
} from '../../utils/applicationSubscriptionCreateUtils';
import { formatSubscriptionSecurityType } from '../../utils/applicationSubscriptionMapper';

function toReferenceSelection(item: SubscriptionSearchResultItem): SubscriptionReferenceSelection {
    if (item.type === 'API') {
        return {
            type: 'API',
            id: item.value.id,
            name: item.value.name,
            version: item.value.apiVersion,
            isFederated: item.value.definitionVersion === 'FEDERATED',
        };
    }
    return {
        type: 'API_PRODUCT',
        id: item.value.id,
        name: item.value.name,
        version: item.value.version,
    };
}

function referenceVersionLabel(reference: SubscriptionReferenceSelection): string | undefined {
    return reference.version;
}

function referenceTypeLabel(reference: SubscriptionReferenceSelection): string {
    return reference.type === 'API' ? 'API' : 'API Product';
}

function searchResultVersion(item: SubscriptionSearchResultItem): string | undefined {
    return item.type === 'API' ? item.value.apiVersion : item.value.version;
}

function searchResultTypeLabel(item: SubscriptionSearchResultItem): string {
    return item.type === 'API' ? 'API' : 'API Product';
}

function searchResultOwner(item: SubscriptionSearchResultItem): string | undefined {
    return item.value.primaryOwner?.displayName;
}

const REFERENCE_SEARCH_DEBOUNCE_MS = 400;

function PlanRadioIndicator({ selected }: Readonly<{ selected: boolean }>) {
    return (
        <span
            className={cn(
                'mt-0.5 flex size-4 shrink-0 items-center justify-center rounded-full border-2',
                selected ? 'border-primary' : 'border-muted-foreground/50',
            )}
            aria-hidden
        >
            {selected ? <span className="size-2 rounded-full bg-primary" /> : null}
        </span>
    );
}

function ApiKeyModeOption({
    label,
    description,
    selected,
    onSelect,
}: Readonly<{
    label: string;
    description: string;
    selected: boolean;
    onSelect: () => void;
}>) {
    return (
        <button
            type="button"
            onClick={onSelect}
            className={cn(
                'flex w-full items-start gap-3 rounded-lg border p-3 text-left transition-colors',
                selected ? 'border-primary/50 bg-primary/5' : 'hover:bg-accent',
            )}
            aria-label={label}
            aria-pressed={selected}
        >
            <PlanRadioIndicator selected={selected} />
            <div className="min-w-0">
                <span className="text-sm font-medium">{label}</span>
                <p className="mt-0.5 text-xs text-muted-foreground">{description}</p>
            </div>
        </button>
    );
}

export function ApplicationSubscriptionCreateDialog({
    application,
    basePath,
    open,
    onOpenChange,
}: Readonly<{
    application: ApplicationListItem;
    basePath: string;
    open: boolean;
    onOpenChange: (open: boolean) => void;
}>) {
    const navigate = useNavigate();
    const [search, setSearch] = useState('');
    const [debouncedSearch, setDebouncedSearch] = useState('');
    const [selectedReference, setSelectedReference] = useState<SubscriptionReferenceSelection | null>(null);
    const [selectedPlan, setSelectedPlan] = useState<SubscribablePlan | null>(null);
    const [request, setRequest] = useState('');
    const [apiKeyMode, setApiKeyMode] = useState<ApiKeyMode | null>(null);
    const [error, setError] = useState<string | null>(null);

    useEffect(() => {
        const timer = window.setTimeout(() => setDebouncedSearch(search.trim()), REFERENCE_SEARCH_DEBOUNCE_MS);
        return () => window.clearTimeout(timer);
    }, [search]);

    const { data: portalConfig } = useEnvironmentPortalConfiguration();
    const canUseSharedApiKeys = isSharedApiKeyEnabled(portalConfig);
    const { data: apiKeySubscriptions = [] } = useApplicationApiKeySubscriptions(application.id, open);
    const { data: searchResults = [], isFetching: isSearching } = useSubscriptionReferenceSearch(debouncedSearch);
    const { data: plansResponse, isLoading: isLoadingPlans } = useSubscribablePlans(selectedReference, application.id);
    const createMutation = useCreateApplicationSubscription(application.id);

    const plans = useMemo(() => plansResponse?.data ?? [], [plansResponse?.data]);

    const showApiKeyModeChoice = useMemo(
        () =>
            shouldShowApiKeyModeChoice({
                applicationApiKeyMode: application.api_key_mode,
                planSecurityType: selectedPlan?.security?.type,
                canUseSharedApiKeys,
                isFederatedApi: selectedReference?.type === 'API' && Boolean(selectedReference.isFederated),
                selectedReference,
                apiKeySubscriptions,
            }),
        [application.api_key_mode, selectedPlan?.security?.type, canUseSharedApiKeys, selectedReference, apiKeySubscriptions],
    );

    const reset = () => {
        setSearch('');
        setDebouncedSearch('');
        setSelectedReference(null);
        setSelectedPlan(null);
        setRequest('');
        setApiKeyMode(null);
        setError(null);
    };

    useEffect(() => {
        if (!open) reset();
    }, [open]);

    useEffect(() => {
        if (!selectedReference) {
            setSelectedPlan(null);
            return;
        }
        if (!selectedPlan || !plans.find(p => p.id === selectedPlan.id)) {
            const firstEnabled = plans.find(plan => !isPlanDisabled(plan));
            setSelectedPlan(firstEnabled ?? null);
        }
    }, [selectedReference, plans, selectedPlan]);

    useEffect(() => {
        if (!showApiKeyModeChoice) {
            setApiKeyMode(null);
        }
    }, [showApiKeyModeChoice, selectedPlan?.id, selectedReference?.id]);

    const handleOpenChange = (next: boolean) => {
        onOpenChange(next);
    };

    const selectReference = (item: SubscriptionSearchResultItem) => {
        const reference = toReferenceSelection(item);
        setSelectedReference(reference);
        setSelectedPlan(null);
        setApiKeyMode(null);
        setSearch(reference.name);
        setError(null);
    };

    const handleCreate = async () => {
        if (!selectedReference || !selectedPlan) return;
        const validationError = getSubscriptionCreateValidationError(selectedPlan, request, showApiKeyModeChoice, apiKeyMode);
        if (validationError) {
            setError(validationError);
            return;
        }

        setError(null);
        try {
            const created = await createMutation.mutateAsync({
                planId: selectedPlan.id,
                payload: buildSubscriptionCreatePayload(request, showApiKeyModeChoice, apiKeyMode),
            });
            handleOpenChange(false);
            if (created.id) {
                navigate(`${basePath}/subscriptions/${created.id}`);
            }
        } catch {
            setError('Failed to create subscription. Please try again.');
        }
    };

    const showResults = search.trim().length > 0 && !selectedReference;
    const referenceLabel = selectedReference ? referenceTypeLabel(selectedReference) : 'API or API Product';
    const createDisabled =
        !selectedReference ||
        !selectedPlan ||
        isPlanDisabled(selectedPlan) ||
        createMutation.isPending ||
        (showApiKeyModeChoice && !apiKeyMode);

    return (
        <Dialog open={open} onOpenChange={handleOpenChange}>
            <DialogContent
                className="gap-4 overflow-hidden sm:max-w-lg"
                style={{ width: 'min(90vw, 32rem)', maxWidth: 'min(90vw, 32rem)' }}
            >
                <DialogHeader className="space-y-1.5 border-b pb-4">
                    <DialogTitle>Create a subscription</DialogTitle>
                    <DialogDescription>
                        Search for an API or API Product, select a plan, and submit a subscription request.
                    </DialogDescription>
                </DialogHeader>

                <div className="space-y-5">
                    <div className="space-y-2">
                        <Label htmlFor="sub-api-search">Search an API or API Product</Label>
                        <div className="relative">
                            <SearchIcon
                                className="pointer-events-none absolute left-3 top-1/2 size-4 -translate-y-1/2 text-muted-foreground"
                                aria-hidden
                            />
                            <Input
                                id="sub-api-search"
                                placeholder="Search by name..."
                                className="pl-9"
                                value={search}
                                onChange={e => {
                                    setSearch(e.target.value);
                                    if (selectedReference && e.target.value !== selectedReference.name) {
                                        setSelectedReference(null);
                                        setSelectedPlan(null);
                                        setApiKeyMode(null);
                                    }
                                }}
                            />
                        </div>
                        {showResults ? (
                            <div className="max-h-[180px] overflow-y-auto rounded-md border">
                                {isSearching ? (
                                    <p className="px-3 py-2 text-sm text-muted-foreground">Searching…</p>
                                ) : searchResults.length === 0 ? (
                                    <p className="px-3 py-2 text-sm text-muted-foreground">No API or API Product found</p>
                                ) : (
                                    searchResults.map(item => {
                                        const version = searchResultVersion(item);
                                        const owner = searchResultOwner(item);
                                        return (
                                            <button
                                                key={`${item.type}-${item.value.id}`}
                                                type="button"
                                                onClick={() => selectReference(item)}
                                                className="flex w-full items-start gap-3 border-b px-3 py-2.5 text-left transition-colors last:border-b-0 hover:bg-accent"
                                            >
                                                <div className="min-w-0 flex-1">
                                                    <p className="truncate text-sm font-medium">
                                                        {item.value.name}
                                                        {version ? ` — ${version}` : ''}
                                                        {owner ? ` (${owner})` : ''}
                                                    </p>
                                                    <p className="text-[11px] text-muted-foreground">{searchResultTypeLabel(item)}</p>
                                                </div>
                                            </button>
                                        );
                                    })
                                )}
                            </div>
                        ) : null}
                        {selectedReference ? (
                            <Badge variant="secondary" className="text-xs">
                                {selectedReference.name}
                                {referenceVersionLabel(selectedReference) ? ` — ${referenceVersionLabel(selectedReference)}` : ''}
                                <span className="text-muted-foreground"> · {referenceTypeLabel(selectedReference)}</span>
                            </Badge>
                        ) : null}
                    </div>

                    {selectedReference ? (
                        <div className="space-y-2">
                            <Label>Select a plan to subscribe</Label>
                            {isLoadingPlans ? (
                                <p className="text-sm text-muted-foreground">Loading plans…</p>
                            ) : plans.length === 0 ? (
                                <p className="text-sm text-muted-foreground">No subscribable plans for this {referenceLabel}.</p>
                            ) : (
                                <div className="max-h-[220px] space-y-2 overflow-y-auto">
                                    {plans.map(plan => {
                                        const disabled = isPlanDisabled(plan);
                                        const reason = planDisabledReason(plan);
                                        const securityLabel = plan.security?.type
                                            ? formatSubscriptionSecurityType(plan.security.type)
                                            : '—';
                                        return (
                                            <button
                                                key={plan.id}
                                                type="button"
                                                disabled={disabled}
                                                onClick={() => !disabled && setSelectedPlan(plan)}
                                                className={cn(
                                                    'flex w-full items-start gap-3 rounded-lg border p-3 text-left transition-colors',
                                                    disabled && 'cursor-not-allowed opacity-60',
                                                    selectedPlan?.id === plan.id && 'border-primary/50 bg-primary/5',
                                                )}
                                                title={reason}
                                                aria-pressed={selectedPlan?.id === plan.id}
                                            >
                                                <PlanRadioIndicator selected={selectedPlan?.id === plan.id} />
                                                <div className="min-w-0 flex-1">
                                                    <div className="flex flex-wrap items-baseline gap-x-2.5 gap-y-1">
                                                        <span className="text-sm font-medium leading-snug text-foreground">
                                                            {plan.name}
                                                        </span>
                                                        <span className="text-xs font-normal leading-snug text-muted-foreground">
                                                            {securityLabel}
                                                        </span>
                                                    </div>
                                                    {reason ? (
                                                        <span className="mt-1.5 block text-xs text-amber-600 dark:text-amber-400">
                                                            {reason}
                                                        </span>
                                                    ) : null}
                                                </div>
                                            </button>
                                        );
                                    })}
                                </div>
                            )}
                        </div>
                    ) : null}

                    {showApiKeyModeChoice ? (
                        <div className="space-y-3">
                            <div className="space-y-1 text-sm">
                                <p>You have to choose between two modes for your application:</p>
                                <ul className="list-disc space-y-0.5 pl-5 text-muted-foreground">
                                    <li>
                                        <span className="font-medium text-foreground">API Key</span> — a new API key will be generated for
                                        each subscription
                                    </li>
                                    <li>
                                        <span className="font-medium text-foreground">Shared API Key</span> — each subscription will use the
                                        same API key
                                    </li>
                                </ul>
                                <p className="text-amber-600 dark:text-amber-400">Please note that this choice is permanent.</p>
                            </div>
                            <div className="space-y-2">
                                <ApiKeyModeOption
                                    label="API Key"
                                    description="Generate a dedicated API key for each subscription"
                                    selected={apiKeyMode === 'EXCLUSIVE'}
                                    onSelect={() => setApiKeyMode('EXCLUSIVE')}
                                />
                                <ApiKeyModeOption
                                    label="Shared API Key"
                                    description="Reuse the same API key across subscriptions"
                                    selected={apiKeyMode === 'SHARED'}
                                    onSelect={() => setApiKeyMode('SHARED')}
                                />
                            </div>
                        </div>
                    ) : null}

                    {selectedPlan?.commentRequired ? (
                        <div className="space-y-2">
                            <Label htmlFor="sub-request">
                                Subscription message <span className="text-destructive">*</span>
                            </Label>
                            <Textarea
                                id="sub-request"
                                value={request}
                                onChange={e => setRequest(e.target.value)}
                                placeholder="Fill a message to the API owner"
                                rows={3}
                            />
                        </div>
                    ) : null}

                    {error ? <p className="text-sm text-destructive">{error}</p> : null}
                </div>

                <DialogFooter className="border-t pt-4 sm:justify-end gap-2">
                    <Button type="button" variant="outline" onClick={() => handleOpenChange(false)}>
                        Cancel
                    </Button>
                    <Button type="button" disabled={createDisabled} onClick={() => void handleCreate()}>
                        {createMutation.isPending ? 'Creating…' : 'Create subscription'}
                    </Button>
                </DialogFooter>
            </DialogContent>
        </Dialog>
    );
}
