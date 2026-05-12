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
import { ArrowLeftIcon, ArrowRightIcon } from '@gravitee/graphene-core/icons';

import type { StepConfig } from '../../../../utils/stepRegistry';

type WizardFooterProps = Readonly<{
    activeStep: StepConfig | undefined;
    activeStepIndex: number;
    totalSteps: number;
    isReview: boolean;
    deployImmediately: boolean;
    canGoBack: boolean;
    isPrimaryDisabled: boolean;
    isPending: boolean;
    isTemplate: boolean;
    onBack: () => void;
    onPrevious: () => void;
    onPrimary: () => void;
}>;

function getPrimaryLabel(stepId: string | undefined, isReview: boolean, deployImmediately: boolean, isPending: boolean): string {
    if (isPending) return 'Creating…';
    if (isReview) return deployImmediately ? 'Deploy Proxy' : 'Create Proxy';
    if (stepId === 'essentials') return 'Review & deploy';
    if (stepId === 'api-details') return 'Continue to entrypoints';
    if (stepId === 'configure-proxy') return 'Continue to security';
    if (stepId === 'secure') return 'Review & deploy';
    return 'Continue';
}

export function WizardFooter({
    activeStep,
    activeStepIndex,
    totalSteps,
    isReview,
    deployImmediately,
    canGoBack,
    isPrimaryDisabled,
    isPending,
    isTemplate,
    onBack,
    onPrevious,
    onPrimary,
}: WizardFooterProps) {
    const primaryLabel = getPrimaryLabel(activeStep?.id, isReview, deployImmediately, isPending);
    const backLabel = isTemplate ? 'Back to templates' : 'Exit wizard';

    return (
        <div className="flex w-full flex-col gap-3 border-t pt-4 sm:flex-row sm:items-center">
            <div className="flex items-center gap-2">
                <Button type="button" variant="outline" size="sm" onClick={onBack}>
                    {backLabel}
                </Button>
                {canGoBack ? (
                    <Button type="button" variant="outline" size="sm" onClick={onPrevious}>
                        <ArrowLeftIcon className="size-4" aria-hidden="true" />
                        Previous
                    </Button>
                ) : null}
            </div>

            <div className="flex flex-1 items-center justify-end gap-3">
                {activeStep ? (
                    <span className="text-xs text-muted-foreground">
                        Step {activeStepIndex + 1} of {totalSteps}
                    </span>
                ) : null}
                <Button type="button" size="sm" disabled={isPrimaryDisabled} onClick={onPrimary}>
                    {primaryLabel}
                    <ArrowRightIcon className="ml-2 size-4" aria-hidden="true" />
                </Button>
            </div>
        </div>
    );
}
