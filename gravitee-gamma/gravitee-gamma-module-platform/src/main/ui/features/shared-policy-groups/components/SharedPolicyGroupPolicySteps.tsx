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
import { DndContext, type DragEndEvent, KeyboardSensor, PointerSensor, closestCenter, useSensor, useSensors } from '@dnd-kit/core';
import { SortableContext, rectSortingStrategy, sortableKeyboardCoordinates, useSortable } from '@dnd-kit/sortable';
import { CSS } from '@dnd-kit/utilities';
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
    CircleCheckIcon,
    CopyIcon,
    EyeOffIcon,
    GripVerticalIcon,
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
import { Fragment, useMemo, useRef, useState } from 'react';

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
    const sensors = useSensors(
        useSensor(PointerSensor, { activationConstraint: { distance: 4 } }),
        useSensor(KeyboardSensor, { coordinateGetter: sortableKeyboardCoordinates }),
    );
    // Ids must stay stable across reorders — dnd-kit tracks a dragged item by id for the whole
    // gesture, and an id derived from array position would silently point at a different step
    // the moment the array is reordered. Keyed by step object identity, which reorders preserve
    // (only add/duplicate/server-resync create new step objects).
    const stepIdsRef = useRef(new WeakMap<ResolvedStep['step'], string>());
    const nextStepIdRef = useRef(0);
    const stepIds = useMemo(
        () =>
            resolvedSteps.map(({ step }) => {
                let id = stepIdsRef.current.get(step);
                if (!id) {
                    id = `step-${nextStepIdRef.current++}`;
                    stepIdsRef.current.set(step, id);
                }
                return id;
            }),
        [resolvedSteps],
    );

    function handleDragEnd(event: DragEndEvent) {
        const { active, over } = event;
        if (!over || active.id === over.id) {
            return;
        }
        const oldIndex = stepIds.indexOf(String(active.id));
        const newIndex = stepIds.indexOf(String(over.id));
        if (oldIndex === -1 || newIndex === -1) {
            return;
        }
        onMove(oldIndex, newIndex);
    }

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
                        <DndContext sensors={sensors} collisionDetection={closestCenter} onDragEnd={handleDragEnd}>
                            <SortableContext items={stepIds} strategy={rectSortingStrategy}>
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
                                                <SortableStepItem
                                                    id={stepIds[index]}
                                                    index={index}
                                                    label={label}
                                                    resolvedStep={resolvedStep}
                                                    stepEnabled={stepEnabled}
                                                    selected={selectedStepIndex === index}
                                                    readOnly={readOnly}
                                                    onSelect={onSelect}
                                                    onDuplicate={onDuplicate}
                                                    onToggleEnabled={onToggleEnabled}
                                                    onRemove={onRemove}
                                                />
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
                            </SortableContext>
                        </DndContext>
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

function SortableStepItem({
    id,
    index,
    label,
    resolvedStep,
    stepEnabled,
    selected,
    readOnly,
    onSelect,
    onDuplicate,
    onToggleEnabled,
    onRemove,
}: {
    readonly id: string;
    readonly index: number;
    readonly label: string;
    readonly resolvedStep: ResolvedStep;
    readonly stepEnabled: boolean;
    readonly selected: boolean;
    readonly readOnly: boolean;
    readonly onSelect: (stepIndex: number) => void;
    readonly onDuplicate: (stepIndex: number) => void;
    readonly onToggleEnabled: (stepIndex: number, enabled: boolean) => void;
    readonly onRemove: (stepIndex: number) => void;
}) {
    const { attributes, listeners, setNodeRef, transform, transition, isDragging } = useSortable({ id, disabled: readOnly });
    const style = {
        transform: CSS.Transform.toString(transform),
        transition,
    };

    return (
        <li
            ref={setNodeRef}
            style={style}
            className={cn(
                'flex max-w-64 shrink-0 items-center rounded-xl border bg-background pr-1 shadow-sm',
                selected && 'border-primary ring-2 ring-primary/15',
                !stepEnabled && 'opacity-60',
                isDragging && 'z-10 opacity-80 shadow-md',
            )}
        >
            {!readOnly ? (
                <button
                    type="button"
                    className="flex shrink-0 cursor-grab touch-none items-center self-stretch px-1 text-muted-foreground hover:text-foreground active:cursor-grabbing"
                    aria-label={`Reorder ${label}`}
                    {...attributes}
                    {...listeners}
                >
                    <GripVerticalIcon className="size-4" aria-hidden />
                </button>
            ) : null}
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
                            {resolvedStep.unresolvedMessage ?? 'This policy is not available in this environment.'}
                        </span>
                    ) : null}
                </span>
            </Button>
            {!readOnly ? (
                <PolicyStepActions
                    label={label}
                    index={index}
                    unresolved={resolvedStep.unresolved}
                    enabled={stepEnabled}
                    onDuplicate={onDuplicate}
                    onToggleEnabled={onToggleEnabled}
                    onRemove={onRemove}
                />
            ) : null}
        </li>
    );
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
    unresolved,
    enabled,
    onDuplicate,
    onToggleEnabled,
    onRemove,
}: {
    readonly label: string;
    readonly index: number;
    readonly unresolved: boolean;
    readonly enabled: boolean;
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
