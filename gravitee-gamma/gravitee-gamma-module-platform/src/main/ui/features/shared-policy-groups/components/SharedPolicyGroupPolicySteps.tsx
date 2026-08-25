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
    DropdownMenu,
    DropdownMenuContent,
    DropdownMenuItem,
    DropdownMenuSeparator,
    DropdownMenuTrigger,
    cn,
} from '@gravitee/graphene-core';
import {
    ArrowRightIcon,
    ChevronDownIcon,
    ChevronUpIcon,
    CircleCheckIcon,
    CopyIcon,
    EyeOffIcon,
    MonitorIcon,
    MoreVerticalIcon,
    PlusIcon,
    ServerIcon,
    Trash2Icon,
} from '@gravitee/graphene-core/icons';
import {
    type ConnectorRole,
    filterPoliciesForPhase,
    getAvailablePolicyCategories,
    getEmptyPhaseWhisper,
    getPhaseConfig,
    getPhaseWhisper,
    type ApiType,
    type FlowPhase,
    type Policy,
    PolicyIcon,
    PolicyQuickInsert,
    type ResolvedStep,
} from '@gravitee/graphene-policy-studio';
import { Fragment, useMemo, useState } from 'react';

interface SharedPolicyGroupPolicyStepsProps {
    readonly apiType: ApiType;
    readonly phase: FlowPhase;
    readonly policies: readonly Policy[];
    readonly resolvedSteps: readonly ResolvedStep[];
    readonly readOnly: boolean;
    readonly selectedStepIndex?: number;
    readonly onSelect: (stepIndex: number) => void;
    readonly onBrowseCatalog: () => void;
    readonly onBrowseCategory: (category: string) => void;
    readonly onQuickAdd: (policy: Policy) => void;
    readonly onMove: (oldIndex: number, newIndex: number) => void;
    readonly onDuplicate: (stepIndex: number) => void;
    readonly onToggleEnabled: (stepIndex: number, enabled: boolean) => void;
    readonly onRemove: (stepIndex: number) => void;
}

function stepLabel({ policy, step, index }: ResolvedStep): string {
    return policy?.name ?? step.name?.trim() ?? `Policy ${index + 1}`;
}

export function SharedPolicyGroupPolicySteps({
    apiType,
    phase,
    policies,
    resolvedSteps,
    readOnly,
    selectedStepIndex,
    onSelect,
    onBrowseCatalog,
    onBrowseCategory,
    onQuickAdd,
    onMove,
    onDuplicate,
    onToggleEnabled,
    onRemove,
}: SharedPolicyGroupPolicyStepsProps) {
    const [quickInsertOpen, setQuickInsertOpen] = useState(false);
    const phaseConfig = getPhaseConfig(phase, apiType);
    const compatiblePolicies = useMemo(() => filterPoliciesForPhase(policies, apiType, phase), [apiType, phase, policies]);
    const availableCategories = useMemo(() => getAvailablePolicyCategories(compatiblePolicies), [compatiblePolicies]);
    const whisper =
        resolvedSteps.length === 0
            ? getEmptyPhaseOutcome(phase, getEmptyPhaseWhisper(phase, apiType, availableCategories))
            : getPhaseWhisper(phase, apiType, resolvedSteps, availableCategories);
    const suggestedCategory = whisper?.suggestedCategory;

    return (
        <section className="space-y-3">
            <div>
                <h2 className="text-base font-semibold">{phaseConfig.title}</h2>
                <p className="text-sm text-muted-foreground">{phaseConfig.description}</p>
            </div>

            <div className="overflow-x-auto rounded-xl border bg-card p-4">
                <div className="space-y-3">
                    <div className="flex flex-wrap items-center gap-2">
                        <ActorPill label={phaseConfig.startActorLabel} role={phaseConfig.startConnectorRole} />
                        <ArrowRightIcon className="size-4 shrink-0 text-muted-foreground" aria-hidden />
                        <ol className="flex flex-wrap items-center gap-2">
                            {resolvedSteps.map((resolvedStep, index) => {
                                const label = stepLabel(resolvedStep);
                                const stepEnabled = resolvedStep.step.enabled !== false;
                                return (
                                    <Fragment key={`${resolvedStep.step.policy ?? 'policy'}-${index}`}>
                                        {index > 0 ? (
                                            <li aria-hidden>
                                                <ArrowRightIcon className="size-4 shrink-0 text-muted-foreground" />
                                            </li>
                                        ) : null}
                                        <li
                                            className={cn(
                                                'flex max-w-64 shrink-0 items-center rounded-xl border bg-background pr-1 shadow-sm',
                                                selectedStepIndex === index && 'border-primary ring-2 ring-primary/15',
                                                !stepEnabled && 'opacity-60',
                                            )}
                                        >
                                            <Button
                                                type="button"
                                                variant="ghost"
                                                title={label}
                                                className="h-auto min-w-0 flex-1 justify-start gap-2 whitespace-normal px-3 py-2"
                                                disabled={resolvedStep.unresolved}
                                                onClick={() => onSelect(index)}
                                            >
                                                <PolicyIcon category={resolvedStep.policy?.category} size="sm" />
                                                <span className="flex min-w-0 flex-col items-start gap-0.5 text-left">
                                                    <span className="line-clamp-2">
                                                        <span className="text-muted-foreground">{index + 1}</span> {label}
                                                    </span>
                                                    {!stepEnabled ? (
                                                        <Badge variant="secondary" className="font-normal">
                                                            Disabled
                                                        </Badge>
                                                    ) : null}
                                                    {resolvedStep.step.condition ? (
                                                        <span className="line-clamp-1 text-xs font-normal text-muted-foreground">
                                                            Condition: {resolvedStep.step.condition}
                                                        </span>
                                                    ) : null}
                                                    {resolvedStep.unresolved ? (
                                                        <span className="line-clamp-2 text-xs font-normal text-muted-foreground">
                                                            {resolvedStep.unresolvedMessage ??
                                                                'This policy is not available in this environment.'}
                                                        </span>
                                                    ) : null}
                                                </span>
                                            </Button>
                                            {!readOnly ? (
                                                <PolicyStepActions
                                                    label={label}
                                                    index={index}
                                                    lastIndex={resolvedSteps.length - 1}
                                                    unresolved={resolvedStep.unresolved}
                                                    enabled={stepEnabled}
                                                    onMove={onMove}
                                                    onDuplicate={onDuplicate}
                                                    onToggleEnabled={onToggleEnabled}
                                                    onRemove={onRemove}
                                                />
                                            ) : null}
                                        </li>
                                    </Fragment>
                                );
                            })}
                            {!readOnly ? (
                                <>
                                    {resolvedSteps.length > 0 ? (
                                        <li aria-hidden>
                                            <ArrowRightIcon className="size-4 shrink-0 text-muted-foreground" />
                                        </li>
                                    ) : null}
                                    <li>
                                        <PolicyQuickInsert
                                            policies={compatiblePolicies}
                                            open={quickInsertOpen}
                                            onOpenChange={setQuickInsertOpen}
                                            onAddPolicy={onQuickAdd}
                                            onBrowseCatalog={onBrowseCatalog}
                                        >
                                            <Button type="button" size="icon" variant="outline" aria-label="Add policy">
                                                <PlusIcon className="size-4" aria-hidden />
                                            </Button>
                                        </PolicyQuickInsert>
                                    </li>
                                </>
                            ) : null}
                        </ol>
                        <ArrowRightIcon className="size-4 shrink-0 text-muted-foreground" aria-hidden />
                        <ActorPill label={phaseConfig.endActorLabel} role={phaseConfig.endConnectorRole} />
                    </div>
                    {whisper ? (
                        <div className="flex flex-wrap items-center gap-2 border-t pt-3 text-sm text-muted-foreground">
                            <span aria-hidden>•</span>
                            <span>{whisper.text}</span>
                            {!readOnly && suggestedCategory ? (
                                <Button
                                    type="button"
                                    size="sm"
                                    variant="outline"
                                    onClick={() => onBrowseCategory(suggestedCategory.toLowerCase())}
                                >
                                    + {formatCategory(suggestedCategory)}
                                </Button>
                            ) : null}
                            {!readOnly ? (
                                <Button type="button" size="sm" variant="ghost" onClick={onBrowseCatalog}>
                                    Browse all...
                                </Button>
                            ) : null}
                        </div>
                    ) : null}
                </div>
            </div>
        </section>
    );
}

function formatCategory(category: string): string {
    return category.charAt(0).toUpperCase() + category.slice(1);
}

// The package's empty-phase whisper describes the phase; Classic states the consequence of leaving it empty.
function getEmptyPhaseOutcome(
    phase: FlowPhase,
    fallback: ReturnType<typeof getEmptyPhaseWhisper>,
): ReturnType<typeof getEmptyPhaseWhisper> {
    const phaseOutcomes: Partial<Record<FlowPhase, string>> = {
        REQUEST: 'Requests reach your backend unprotected',
        RESPONSE: 'Responses pass through untransformed',
        PUBLISH: 'Messages are published without content filtering',
        SUBSCRIBE: 'Messages are delivered in raw broker format',
        ENTRYPOINT_CONNECT: 'Connections are unprotected',
        INTERACT: 'Messages pass unfiltered',
    };
    const text = phaseOutcomes[phase];
    return text && fallback ? { ...fallback, text } : fallback;
}

function ActorPill({ label, role }: { readonly label: string; readonly role: ConnectorRole }) {
    const Icon = role === 'entrypoint' ? MonitorIcon : ServerIcon;
    return (
        <div className="flex min-w-28 shrink-0 items-center justify-center gap-2 rounded-xl bg-muted px-4 py-2.5 text-sm font-medium">
            <Icon className="size-4" aria-hidden />
            {label}
        </div>
    );
}

function PolicyStepActions({
    label,
    index,
    lastIndex,
    unresolved,
    enabled,
    onMove,
    onDuplicate,
    onToggleEnabled,
    onRemove,
}: {
    readonly label: string;
    readonly index: number;
    readonly lastIndex: number;
    readonly unresolved: boolean;
    readonly enabled: boolean;
    readonly onMove: (oldIndex: number, newIndex: number) => void;
    readonly onDuplicate: (stepIndex: number) => void;
    readonly onToggleEnabled: (stepIndex: number, enabled: boolean) => void;
    readonly onRemove: (stepIndex: number) => void;
}) {
    return (
        <DropdownMenu>
            <DropdownMenuTrigger asChild>
                <Button type="button" size="icon" variant="ghost" className="size-8 shrink-0" aria-label={`${label} actions`}>
                    <MoreVerticalIcon className="size-4" aria-hidden />
                </Button>
            </DropdownMenuTrigger>
            <DropdownMenuContent align="end">
                <DropdownMenuItem disabled={index === 0} onSelect={() => onMove(index, index - 1)}>
                    <ChevronUpIcon className="mr-2 size-4" aria-hidden />
                    Move up
                </DropdownMenuItem>
                <DropdownMenuItem disabled={index === lastIndex} onSelect={() => onMove(index, index + 1)}>
                    <ChevronDownIcon className="mr-2 size-4" aria-hidden />
                    Move down
                </DropdownMenuItem>
                <DropdownMenuItem disabled={unresolved} onSelect={() => onDuplicate(index)}>
                    <CopyIcon className="mr-2 size-4" aria-hidden />
                    Duplicate
                </DropdownMenuItem>
                <DropdownMenuItem disabled={unresolved} onSelect={() => onToggleEnabled(index, !enabled)}>
                    {enabled ? <EyeOffIcon className="mr-2 size-4" aria-hidden /> : <CircleCheckIcon className="mr-2 size-4" aria-hidden />}
                    {enabled ? 'Disable' : 'Enable'}
                </DropdownMenuItem>
                <DropdownMenuSeparator />
                <DropdownMenuItem variant="destructive" onSelect={() => onRemove(index)}>
                    <Trash2Icon className="mr-2 size-4" aria-hidden />
                    Remove
                </DropdownMenuItem>
            </DropdownMenuContent>
        </DropdownMenu>
    );
}
