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
import { Button, Card, Tooltip, TooltipContent, TooltipProvider, TooltipTrigger } from '@gravitee/graphene-core';
import { RocketIcon } from '@gravitee/graphene-core/icons';
import {
    buildGenericPolicies,
    type Policy,
    PolicyCatalogPane,
    PolicyConfigPanel,
    PolicyStudioProvider,
    resolveStep,
    type Step,
} from '@gravitee/graphene-policy-studio';
import { useMemo, useState } from 'react';

import { SharedPolicyGroupPolicySteps } from './SharedPolicyGroupPolicySteps';
import type { SharedPolicyGroup, SharedPolicyGroupStep } from '../types/sharedPolicyGroup';

type StudioPanel = { type: 'catalog'; initialCategories?: string[] } | { type: 'configuration'; stepIndex: number } | null;

const EMPTY_LIST = [] as const;

// A Shared Policy Group is a single phase with no flows, so the studio's own flow-level save has nothing
// to persist — this component owns the save action instead.
const NOOP_SAVE = () => undefined;

function serverStepsRevision(sharedPolicyGroup: SharedPolicyGroup): string {
    return `${sharedPolicyGroup.version ?? ''}:${sharedPolicyGroup.updatedAt ?? ''}:${JSON.stringify(sharedPolicyGroup.steps ?? [])}`;
}

interface SharedPolicyGroupPolicyStudioProps {
    readonly sharedPolicyGroup: SharedPolicyGroup;
    readonly policies: readonly Policy[];
    readonly readOnly: boolean;
    readonly isDeploying?: boolean;
    readonly onSave?: (steps: SharedPolicyGroupStep[]) => Promise<SharedPolicyGroup>;
    readonly onDeploy?: () => Promise<void>;
    readonly onFetchPolicySchema: (policy: Policy) => Promise<unknown>;
    readonly onFetchPolicyDocumentation: (policy: Policy) => Promise<string>;
}

export function SharedPolicyGroupPolicyStudio({
    sharedPolicyGroup,
    policies,
    readOnly,
    isDeploying = false,
    onSave,
    onDeploy,
    onFetchPolicySchema,
    onFetchPolicyDocumentation,
}: SharedPolicyGroupPolicyStudioProps) {
    const [steps, setSteps] = useState<Step[]>(sharedPolicyGroup.steps ?? []);
    const [panel, setPanel] = useState<StudioPanel>(null);
    const [hasChanges, setHasChanges] = useState(false);
    const [isSaving, setIsSaving] = useState(false);
    const serverRevision = serverStepsRevision(sharedPolicyGroup);
    const [syncedServerRevision, setSyncedServerRevision] = useState(serverRevision);
    const [savedLifecycle, setSavedLifecycle] = useState<{
        sourceRevision: string;
        lifecycleState: SharedPolicyGroup['lifecycleState'];
    }>();

    if (!hasChanges && syncedServerRevision !== serverRevision) {
        setSteps(sharedPolicyGroup.steps ?? []);
        setPanel(null);
        setSyncedServerRevision(serverRevision);
    }

    const genericPolicies = useMemo(() => buildGenericPolicies(policies, EMPTY_LIST), [policies]);
    const resolvedSteps = useMemo(() => steps.map((step, index) => resolveStep(step, index, genericPolicies)), [steps, genericPolicies]);

    const selectedStepIndex = panel?.type === 'configuration' ? panel.stepIndex : undefined;
    const selectedStep = selectedStepIndex === undefined ? undefined : steps[selectedStepIndex];
    const selectedGenericPolicy = selectedStepIndex === undefined ? undefined : resolvedSteps[selectedStepIndex]?.policy;
    const selectedPolicy = selectedGenericPolicy ? policies.find(policy => policy.id === selectedGenericPolicy.policyId) : undefined;
    const lifecyclePending = isDeploying;
    const lifecycleState =
        savedLifecycle?.sourceRevision === serverRevision ? savedLifecycle.lifecycleState : sharedPolicyGroup.lifecycleState;

    function replaceSteps(nextSteps: Step[]) {
        setSteps(nextSteps);
        setHasChanges(true);
    }

    function handleAddPolicy(policy: Policy) {
        const stepIndex = steps.length;
        replaceSteps([...steps, { policy: policy.id, name: policy.name, enabled: true, configuration: {} }]);
        setPanel({ type: 'configuration', stepIndex });
    }

    function handleReorderSteps(oldIndex: number, newIndex: number) {
        if (oldIndex === newIndex || oldIndex < 0 || newIndex < 0 || oldIndex >= steps.length || newIndex >= steps.length) {
            return;
        }
        const reorderedSteps = [...steps];
        const [movedStep] = reorderedSteps.splice(oldIndex, 1);
        reorderedSteps.splice(newIndex, 0, movedStep);
        replaceSteps(reorderedSteps);
        setPanel(null);
    }

    function handleRemoveStep(stepIndex: number) {
        replaceSteps(steps.filter((_, index) => index !== stepIndex));
        setPanel(null);
    }

    function handleDuplicateStep(stepIndex: number) {
        const step = steps[stepIndex];
        if (!step) {
            return;
        }
        const duplicatedStep = { ...step, name: step.name ? `${step.name} copy` : undefined };
        replaceSteps([...steps.slice(0, stepIndex + 1), duplicatedStep, ...steps.slice(stepIndex + 1)]);
        setPanel({ type: 'configuration', stepIndex: stepIndex + 1 });
    }

    function handleStepChange(stepIndex: number, patch: Partial<Step>) {
        replaceSteps(steps.map((step, index) => (index === stepIndex ? { ...step, ...patch } : step)));
    }

    async function handleSave() {
        if (!onSave) {
            return;
        }
        setIsSaving(true);
        try {
            const updatedSharedPolicyGroup = await onSave(steps.map(step => ({ ...step })));
            setSavedLifecycle({
                sourceRevision: serverRevision,
                lifecycleState: updatedSharedPolicyGroup.lifecycleState,
            });
            setHasChanges(false);
        } catch {
            // The page owns user-facing API error notification.
        } finally {
            setIsSaving(false);
        }
    }

    return (
        <PolicyStudioProvider
            apiType={sharedPolicyGroup.apiType}
            policies={policies}
            sharedPolicyGroups={EMPTY_LIST}
            plans={EMPTY_LIST}
            commonFlows={EMPTY_LIST}
            entrypointsInfo={EMPTY_LIST}
            endpointsInfo={EMPTY_LIST}
            flowExecution={{ mode: 'DEFAULT' }}
            readOnly={readOnly}
            onSave={NOOP_SAVE}
            onFetchPolicySchema={onFetchPolicySchema}
            onFetchPolicyDocumentation={onFetchPolicyDocumentation}
        >
            <Card className="overflow-hidden">
                {!readOnly ? (
                    <div className="flex flex-wrap items-center justify-end gap-2 border-b p-3">
                        <TooltipProvider delayDuration={200}>
                            <Tooltip>
                                <TooltipTrigger asChild>
                                    <span className="inline-flex">
                                        <Button
                                            type="button"
                                            size="sm"
                                            variant="outline"
                                            disabled={!hasChanges || isSaving}
                                            onClick={() => void handleSave()}
                                        >
                                            {isSaving ? 'Saving…' : 'Save policies'}
                                        </Button>
                                    </span>
                                </TooltipTrigger>
                                <TooltipContent>
                                    Don&apos;t forget to ensure that the Prerequisite message is updated with the latest changes.
                                </TooltipContent>
                            </Tooltip>
                        </TooltipProvider>
                        <Button
                            type="button"
                            size="sm"
                            disabled={!onDeploy || lifecycleState === 'DEPLOYED' || hasChanges || lifecyclePending}
                            onClick={() => void onDeploy?.()}
                        >
                            <RocketIcon className="size-4" aria-hidden />
                            {isDeploying ? 'Deploying…' : 'Deploy'}
                        </Button>
                    </div>
                ) : null}
                <div className="min-w-0">
                    {panel?.type === 'catalog' ? (
                        <PolicyCatalogPane
                            className="min-h-[36rem]"
                            policies={policies}
                            apiType={sharedPolicyGroup.apiType}
                            flowPhase={sharedPolicyGroup.phase}
                            initialCategories={panel.initialCategories}
                            docsLayout="detail"
                            onAddPolicy={handleAddPolicy}
                            onClose={() => setPanel(null)}
                        />
                    ) : null}
                    {panel?.type === 'configuration' && selectedStep && selectedPolicy ? (
                        <PolicyConfigPanel
                            className="min-h-[36rem]"
                            policy={selectedPolicy}
                            step={selectedStep}
                            flowPhase={sharedPolicyGroup.phase}
                            docsLayout="side-by-side"
                            showBackButton
                            onClose={() => setPanel(null)}
                            onRemove={readOnly ? undefined : () => handleRemoveStep(panel.stepIndex)}
                            onDuplicate={readOnly ? undefined : () => handleDuplicateStep(panel.stepIndex)}
                            onToggleEnabled={readOnly ? undefined : enabled => handleStepChange(panel.stepIndex, { enabled })}
                            onStepChange={readOnly ? undefined : patch => handleStepChange(panel.stepIndex, patch)}
                        />
                    ) : null}
                    {!panel ? (
                        <div className="min-w-0 p-6">
                            <SharedPolicyGroupPolicySteps
                                apiType={sharedPolicyGroup.apiType}
                                phase={sharedPolicyGroup.phase}
                                policies={policies}
                                resolvedSteps={resolvedSteps}
                                readOnly={readOnly}
                                selectedStepIndex={selectedStepIndex}
                                onSelect={stepIndex => setPanel({ type: 'configuration', stepIndex })}
                                onBrowseCatalog={() => setPanel({ type: 'catalog' })}
                                onBrowseCategory={category => setPanel({ type: 'catalog', initialCategories: [category] })}
                                onQuickAdd={handleAddPolicy}
                                onMove={handleReorderSteps}
                                onDuplicate={handleDuplicateStep}
                                onToggleEnabled={(stepIndex, enabled) => handleStepChange(stepIndex, { enabled })}
                                onRemove={handleRemoveStep}
                            />
                        </div>
                    ) : null}
                </div>
            </Card>
        </PolicyStudioProvider>
    );
}
