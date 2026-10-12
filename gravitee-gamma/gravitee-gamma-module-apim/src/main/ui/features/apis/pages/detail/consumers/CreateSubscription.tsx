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
    Label,
    RadioGroup,
    RadioGroupItem,
    Select,
    SelectContent,
    SelectItem,
    SelectTrigger,
    SelectValue,
    Separator,
    Sheet,
    SheetContent,
    SheetDescription,
    SheetFooter,
    SheetHeader,
    SheetTitle,
} from '@gravitee/graphene-core';
import { PlusIcon } from '@gravitee/graphene-core/icons';
import { useCallback, useEffect, useState } from 'react';

import { ApplicationSearchList } from './ApplicationSearchList';
import { shouldShowApiKeyModeChoice } from './createSubscriptionApiKeyMode';
import { CustomApiKeyField } from './CustomApiKeyField';
import { useCustomApiKeyCheck } from '../../../hooks/useCustomApiKeyCheck';
import { useCustomApiKeyEnabled, useSharedApiKeyEnabled } from '../../../hooks/usePlanSecuritySettings';
import { useApiPlans, useApplicationApiKeySubscriptions } from '../../../hooks/useSubscriptions';
import type { ApiKeyMode, Application, Plan, SubscriptionContext } from '../../../types/subscription';

interface CreateSubscriptionProps {
    ctx: SubscriptionContext;
    open: boolean;
    isPending: boolean;
    error: string | null;
    isFederated?: boolean;
    onConfirm: (applicationId: string, planId: string, options?: { customApiKey?: string; apiKeyMode?: ApiKeyMode }) => void;
    onClose: () => void;
}

function SubscriptionSummary({ app, plan }: { app: Application; plan: Plan }) {
    return (
        <div className="rounded-lg border p-4 space-y-3">
            <p className="text-sm font-medium">Subscription Summary</p>
            <dl className="space-y-3 text-sm">
                <div className="min-w-0">
                    <dt className="text-xs text-muted-foreground">Application</dt>
                    <dd className="font-medium" style={{ overflowWrap: 'anywhere' }}>
                        {app.name}
                    </dd>
                </div>
                <div className="min-w-0">
                    <dt className="text-xs text-muted-foreground">Plan</dt>
                    <dd className="flex min-w-0 flex-wrap items-center gap-1.5">
                        <span className="min-w-0 font-medium" style={{ overflowWrap: 'anywhere' }}>
                            {plan.name}
                        </span>
                        {plan.security?.type && (
                            <Badge variant="secondary" className="shrink-0 text-xs">
                                {plan.security.type}
                            </Badge>
                        )}
                    </dd>
                </div>
                {app.primaryOwner?.displayName && (
                    <div className="min-w-0">
                        <dt className="text-xs text-muted-foreground">Owner</dt>
                        <dd style={{ overflowWrap: 'anywhere' }}>{app.primaryOwner.displayName}</dd>
                    </div>
                )}
                {app.type && (
                    <div className="min-w-0">
                        <dt className="text-xs text-muted-foreground">Type</dt>
                        <dd>
                            <Badge variant="secondary" className="text-xs">
                                {app.type}
                            </Badge>
                        </dd>
                    </div>
                )}
            </dl>
        </div>
    );
}

export function CreateSubscription({
    ctx,
    open,
    isPending,
    error,
    isFederated = false,
    onConfirm,
    onClose,
}: Readonly<CreateSubscriptionProps>) {
    const [selectedApp, setSelectedApp] = useState<Application | null>(null);
    const [selectedPlanId, setSelectedPlanId] = useState('');
    const [customApiKey, setCustomApiKey] = useState('');
    const [apiKeyMode, setApiKeyMode] = useState<ApiKeyMode | ''>('');

    const customApiKeySetting = useCustomApiKeyEnabled();
    const sharedApiKeySetting = useSharedApiKeyEnabled();
    const canUseCustomApiKey = !isFederated && customApiKeySetting;
    const canUseSharedApiKeys = !isFederated && sharedApiKeySetting;

    // The page closes the sheet itself once a subscription is created, so the form is cleared on open
    // rather than in a close handler that only the Cancel and dismiss paths go through.
    const [wasOpen, setWasOpen] = useState(open);
    if (wasOpen !== open) {
        setWasOpen(open);
        if (open) {
            setSelectedApp(null);
            setSelectedPlanId('');
            setCustomApiKey('');
            setApiKeyMode('');
        }
    }

    const entityLabel = ctx.type === 'api-product' ? 'API product' : 'API';
    const { data: plans = [], isLoading: isLoadingPlans } = useApiPlans(ctx);

    const onlyKeyless = !isLoadingPlans && plans.length === 0;
    const selectedPlan = plans.find(p => p.id === selectedPlanId) ?? null;
    const isApiKeyPlan = selectedPlan?.security?.type === 'API_KEY';

    const modeLookupEnabled = Boolean(selectedApp?.id && isApiKeyPlan && canUseSharedApiKeys && selectedApp.apiKeyMode === 'UNSPECIFIED');
    const {
        data: apiKeySubscriptions = [],
        isFetching: modeLookupFetching,
        isError: modeLookupError,
        isFetched: modeLookupFetched,
    } = useApplicationApiKeySubscriptions(selectedApp?.id, modeLookupEnabled);
    const modeLookupPending = modeLookupEnabled && (modeLookupFetching || !modeLookupFetched);
    const modeLookupUnavailable = modeLookupEnabled && modeLookupError;

    const showApiKeyModeChoice =
        !modeLookupUnavailable &&
        shouldShowApiKeyModeChoice({
            applicationApiKeyMode: selectedApp?.apiKeyMode,
            isApiKeyPlan,
            canUseSharedApiKeys,
            isFederated,
            ctx,
            apiKeySubscriptions,
        });

    const appUsesSharedApiKey = selectedApp?.apiKeyMode === 'SHARED';
    const selectedSharedApiKeyMode = showApiKeyModeChoice && apiKeyMode === 'SHARED';
    const effectiveShared = appUsesSharedApiKey || selectedSharedApiKeyMode;
    const showCustomApiKey =
        Boolean(selectedApp) &&
        canUseCustomApiKey &&
        isApiKeyPlan &&
        !effectiveShared &&
        !modeLookupPending &&
        !modeLookupUnavailable &&
        (!showApiKeyModeChoice || apiKeyMode === 'EXCLUSIVE');
    const customKeyCheck = useCustomApiKeyCheck(ctx, selectedApp?.id, showCustomApiKey ? customApiKey : '');

    const canSubmit = Boolean(
        selectedApp &&
            selectedPlanId &&
            !onlyKeyless &&
            !modeLookupPending &&
            !customKeyCheck.blocked &&
            (!showApiKeyModeChoice || apiKeyMode),
    );

    useEffect(() => {
        setApiKeyMode('');
        setCustomApiKey('');
    }, [selectedApp?.id, selectedPlanId]);

    const handleConfirm = useCallback(() => {
        if (!canSubmit || !selectedApp || customKeyCheck.blocked) return;
        onConfirm(selectedApp.id, selectedPlanId, {
            customApiKey: showCustomApiKey && customApiKey !== '' ? customApiKey : undefined,
            apiKeyMode: showApiKeyModeChoice && apiKeyMode ? apiKeyMode : undefined,
        });
    }, [
        canSubmit,
        selectedApp,
        selectedPlanId,
        showCustomApiKey,
        customApiKey,
        showApiKeyModeChoice,
        apiKeyMode,
        customKeyCheck.blocked,
        onConfirm,
    ]);

    return (
        <Sheet open={open} onOpenChange={open ? onClose : undefined}>
            <SheetContent side="right" style={{ maxWidth: '480px' }}>
                <SheetHeader>
                    <SheetTitle>Create Subscription</SheetTitle>
                    <SheetDescription>
                        Select an environment-level application and assign a plan to subscribe it to this {entityLabel}.
                    </SheetDescription>
                </SheetHeader>

                <div className="space-y-5 px-4" style={{ overflowY: 'auto', flex: 1, minHeight: 0 }}>
                    <div className="space-y-2">
                        <Label>Select Application</Label>
                        <ApplicationSearchList selected={selectedApp} onSelect={app => setSelectedApp(app)} />
                    </div>

                    <Separator />

                    <div className="space-y-2">
                        <Label htmlFor="sub-plan">Subscription Plan</Label>
                        <Select value={selectedPlanId} onValueChange={setSelectedPlanId} disabled={isLoadingPlans || onlyKeyless}>
                            <SelectTrigger id="sub-plan" className="w-full">
                                <SelectValue
                                    placeholder={
                                        isLoadingPlans ? 'Loading plans…' : onlyKeyless ? 'No subscribable plans' : 'Select a plan'
                                    }
                                />
                            </SelectTrigger>
                            <SelectContent>
                                {plans.map(p => (
                                    <SelectItem key={p.id} value={p.id}>
                                        {p.name}
                                        {p.description ? ` — ${p.description}` : ''}
                                    </SelectItem>
                                ))}
                            </SelectContent>
                        </Select>
                        {onlyKeyless && (
                            <p className="text-xs text-muted-foreground">
                                This API only offers keyless plans. Subscriptions are not required.
                            </p>
                        )}
                        {!onlyKeyless && (
                            <p className="text-xs text-muted-foreground">
                                Plans define rate limits, quotas, and access policies for this subscription.
                            </p>
                        )}
                    </div>

                    {showApiKeyModeChoice && (
                        <div className="space-y-3">
                            <div className="space-y-1 text-sm text-muted-foreground">
                                <p>You have to choose between two modes for your application:</p>
                                <ul className="list-disc pl-5 space-y-1">
                                    <li>
                                        <span className="font-medium text-foreground">API Key</span> — a new API Key will be generated for
                                        each subscription
                                    </li>
                                    <li>
                                        <span className="font-medium text-foreground">Shared API Key</span> — each subscription will use the
                                        same API Key
                                    </li>
                                </ul>
                                <p className="text-xs">Please note that this choice is permanent.</p>
                            </div>
                            <RadioGroup
                                value={apiKeyMode}
                                onValueChange={value => setApiKeyMode(value as ApiKeyMode)}
                                className="gap-3"
                                aria-label="API Key Mode"
                            >
                                <div className="flex items-center gap-2">
                                    <RadioGroupItem value="EXCLUSIVE" id="api-key-mode-exclusive" />
                                    <Label htmlFor="api-key-mode-exclusive" className="font-normal">
                                        API Key
                                    </Label>
                                </div>
                                <div className="flex items-center gap-2">
                                    <RadioGroupItem value="SHARED" id="api-key-mode-shared" />
                                    <Label htmlFor="api-key-mode-shared" className="font-normal">
                                        Shared API Key
                                    </Label>
                                </div>
                            </RadioGroup>
                        </div>
                    )}

                    {modeLookupUnavailable && !modeLookupPending && (
                        <p className="text-xs text-muted-foreground">Could not check existing API keys for this application.</p>
                    )}

                    {showCustomApiKey && (
                        <CustomApiKeyField
                            id="sub-custom-api-key"
                            label="Custom API Key"
                            value={customApiKey}
                            onChange={setCustomApiKey}
                            error={customKeyCheck.error}
                            checking={customKeyCheck.checking}
                            disabled={isPending}
                            placeholder="Leave blank to generate an API Key"
                            description="You can provide a custom API Key if you already have one. Leave it blank to get a generated API Key."
                        />
                    )}

                    {selectedApp && selectedPlan && <SubscriptionSummary app={selectedApp} plan={selectedPlan} />}

                    {error && (
                        <Alert variant="destructive">
                            <AlertDescription>{error}</AlertDescription>
                        </Alert>
                    )}
                </div>

                <SheetFooter className="flex-row justify-end border-t">
                    <Button type="button" variant="outline" onClick={onClose} disabled={isPending}>
                        Cancel
                    </Button>
                    <Button type="button" onClick={handleConfirm} disabled={!canSubmit || isPending || customKeyCheck.blocked}>
                        {!isPending && <PlusIcon className="size-4" aria-hidden />}
                        {isPending ? 'Creating…' : 'Create subscription'}
                    </Button>
                </SheetFooter>
            </SheetContent>
        </Sheet>
    );
}
