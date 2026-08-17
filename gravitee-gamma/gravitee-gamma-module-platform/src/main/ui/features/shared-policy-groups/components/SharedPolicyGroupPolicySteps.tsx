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
import { Button } from '@gravitee/graphene-core';
import { ChevronDownIcon, ChevronUpIcon, CopyIcon, PlusIcon, Trash2Icon } from '@gravitee/graphene-core/icons';
import type { ResolvedStep } from '@gravitee/graphene-policy-studio';

interface SharedPolicyGroupPolicyStepsProps {
    readonly resolvedSteps: readonly ResolvedStep[];
    readonly readOnly: boolean;
    readonly selectedStepIndex?: number;
    readonly onSelect: (stepIndex: number) => void;
    readonly onAdd: () => void;
    readonly onMove: (oldIndex: number, newIndex: number) => void;
    readonly onDuplicate: (stepIndex: number) => void;
    readonly onRemove: (stepIndex: number) => void;
}

function stepLabel({ policy, step, index }: ResolvedStep): string {
    return policy?.name ?? step.name?.trim() ?? `Policy ${index + 1}`;
}

export function SharedPolicyGroupPolicySteps({
    resolvedSteps,
    readOnly,
    selectedStepIndex,
    onSelect,
    onAdd,
    onMove,
    onDuplicate,
    onRemove,
}: SharedPolicyGroupPolicyStepsProps) {
    return (
        <div className="space-y-4">
            <div className="flex items-center justify-between gap-3">
                <div>
                    <h2 className="text-base font-semibold">Configured policies</h2>
                    <p className="text-sm text-muted-foreground">Policies run in the order shown below.</p>
                </div>
                {!readOnly ? (
                    <Button type="button" size="sm" className="gap-1.5" onClick={onAdd}>
                        <PlusIcon className="size-4" aria-hidden />
                        Add policy
                    </Button>
                ) : null}
            </div>

            {resolvedSteps.length === 0 ? (
                <div className="rounded-lg border border-dashed p-8 text-center">
                    <p className="font-medium">No policies configured</p>
                    <p className="mt-1 text-sm text-muted-foreground">Add a policy to configure this Shared Policy Group.</p>
                </div>
            ) : (
                <ol className="space-y-2">
                    {resolvedSteps.map((resolvedStep, index) => {
                        const label = stepLabel(resolvedStep);
                        return (
                            <li
                                key={`${resolvedStep.step.policy ?? 'policy'}-${index}`}
                                className={selectedStepIndex === index ? 'rounded-lg border border-primary' : 'rounded-lg border'}
                            >
                                <div className="flex items-center gap-2 p-3">
                                    <Button
                                        type="button"
                                        variant="ghost"
                                        className="h-auto flex-1 justify-start"
                                        // An unresolved step has no schema to render, so configuring it would dead-end.
                                        disabled={resolvedStep.unresolved}
                                        onClick={() => onSelect(index)}
                                    >
                                        <span className="mr-3 text-muted-foreground">{index + 1}</span>
                                        <span className="flex flex-col items-start gap-0.5 text-left">
                                            <span>{label}</span>
                                            {resolvedStep.unresolved ? (
                                                <span className="text-xs font-normal text-muted-foreground">
                                                    {resolvedStep.unresolvedMessage ?? 'This policy is not available in this environment.'}
                                                </span>
                                            ) : null}
                                        </span>
                                    </Button>
                                    {!readOnly ? (
                                        <>
                                            <Button
                                                type="button"
                                                size="icon"
                                                variant="ghost"
                                                aria-label={`Move ${label} up`}
                                                disabled={index === 0}
                                                onClick={() => onMove(index, index - 1)}
                                            >
                                                <ChevronUpIcon className="size-4" aria-hidden />
                                            </Button>
                                            <Button
                                                type="button"
                                                size="icon"
                                                variant="ghost"
                                                aria-label={`Move ${label} down`}
                                                disabled={index === resolvedSteps.length - 1}
                                                onClick={() => onMove(index, index + 1)}
                                            >
                                                <ChevronDownIcon className="size-4" aria-hidden />
                                            </Button>
                                            <Button
                                                type="button"
                                                size="icon"
                                                variant="ghost"
                                                aria-label={`Duplicate ${label}`}
                                                disabled={resolvedStep.unresolved}
                                                onClick={() => onDuplicate(index)}
                                            >
                                                <CopyIcon className="size-4" aria-hidden />
                                            </Button>
                                            <Button
                                                type="button"
                                                size="icon"
                                                variant="ghost"
                                                aria-label={`Remove ${label}`}
                                                onClick={() => onRemove(index)}
                                            >
                                                <Trash2Icon className="size-4" aria-hidden />
                                            </Button>
                                        </>
                                    ) : null}
                                </div>
                            </li>
                        );
                    })}
                </ol>
            )}
        </div>
    );
}
