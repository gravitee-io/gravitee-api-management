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
import type { Application } from '../../editor/entities/application';
import type { Plan } from '../../editor/entities/plan';
import type { SubscriptionConsumerConfiguration } from '../../editor/entities/subscription';

export enum SubscribeStep {
    PLAN_SELECTION = 'PLAN_SELECTION',
    APP_SELECTION = 'APP_SELECTION',
    PUSH_DETAILS = 'PUSH_DETAILS',
    REVIEW = 'REVIEW',
}

export interface ConsumerFormState {
    callbackUrl: string;
    isValid: boolean;
}

export interface WizardState {
    currentStep: SubscribeStep;
    selectedPlan: Plan | null;
    selectedApplication: Application | null;
    consumerConfig: ConsumerFormState;
    apiKeyMode: 'EXCLUSIVE' | 'SHARED' | null;
    subscriptionInProgress: boolean;
    subscriptionError: boolean;
    completedSubscriptionId: string | null;
}

export function getActiveSteps(plan: Plan | null): SubscribeStep[] {
    const steps: SubscribeStep[] = [SubscribeStep.PLAN_SELECTION];

    if (plan?.security !== 'KEY_LESS') {
        steps.push(SubscribeStep.APP_SELECTION);
        if (plan?.mode === 'PUSH') {
            steps.push(SubscribeStep.PUSH_DETAILS);
        }
    }

    steps.push(SubscribeStep.REVIEW);
    return steps;
}

export function stepNumberOf(step: SubscribeStep, activeSteps: SubscribeStep[]): number {
    return activeSteps.indexOf(step) + 1;
}

export function toConsumerConfiguration(form: ConsumerFormState): SubscriptionConsumerConfiguration {
    return {
        entrypointId: 'webhook',
        channel: 'http',
        entrypointConfiguration: {
            callbackUrl: form.callbackUrl,
            headers: [],
            auth: { type: 'none' },
            ssl: { trustAll: false },
            retry: { retryOption: 'Retry On Fail' },
        },
    };
}
