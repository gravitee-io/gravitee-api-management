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
import { useCallback, useEffect, useMemo, useState } from 'react';

import type { Api } from '../../features/editor/entities/api';
import type { Application } from '../../features/editor/entities/application';
import type { Plan } from '../../features/editor/entities/plan';
import { listApplications } from '../../features/editor/services/applications.service';
import { getPlansForApi } from '../../features/editor/services/plans.mock';
import {
    createSubscription,
    getActiveSubscriptionsForApi,
} from '../../features/editor/services/subscriptions.service';
import { usePortalPageOptional } from '../../features/portal-shell/context/PortalPageContext';

import { StepHeader } from './StepHeader';
import { ChooseApplication } from './steps/ChooseApplication';
import { ChoosePlan } from './steps/ChoosePlan';
import { ConfigureConsumer } from './steps/ConfigureConsumer';
import { ReviewAndSubscribe } from './steps/ReviewAndSubscribe';
import {
    SubscribeStep,
    getActiveSteps,
    stepNumberOf,
    toConsumerConfiguration,
    type ConsumerFormState,
} from './types';
import styles from './SubscriptionFlowBlock.module.scss';

const APPLICATIONS_PAGE_SIZE = 4;

interface SubscriptionFlowViewProps {
    readonly isEditable?: boolean;
    readonly apiIdOverride?: string;
}

export function SubscriptionFlowView({ isEditable = false, apiIdOverride }: SubscriptionFlowViewProps) {
    const portalPage = usePortalPageOptional();
    const apiNavItem = portalPage?.apiNavItem;
    const resolvedApiId = apiIdOverride ?? apiNavItem?.apiId ?? null;
    const resolvedApiName = apiNavItem?.title ?? 'API';

    const [api, setApi] = useState<Api | null>(null);
    const [plans, setPlans] = useState<Plan[]>([]);
    const [loading, setLoading] = useState(true);

    const [currentStep, setCurrentStep] = useState(SubscribeStep.PLAN_SELECTION);
    const [selectedPlan, setSelectedPlan] = useState<Plan | null>(null);
    const [selectedApplication, setSelectedApplication] = useState<Application | null>(null);
    const [consumerConfig, setConsumerConfig] = useState<ConsumerFormState>({ callbackUrl: '', isValid: false });
    const [apiKeyMode, setApiKeyMode] = useState<'EXCLUSIVE' | 'SHARED' | null>(null);
    const [subscriptionInProgress, setSubscriptionInProgress] = useState(false);
    const [subscriptionError, setSubscriptionError] = useState(false);
    const [completedSubscriptionId, setCompletedSubscriptionId] = useState<string | null>(null);

    const [applications, setApplications] = useState<Application[]>([]);
    const [applicationsPage, setApplicationsPage] = useState(1);
    const [applicationsTotalPages, setApplicationsTotalPages] = useState(1);
    const [applicationsTotal, setApplicationsTotal] = useState(0);
    const [disabledApplicationIds, setDisabledApplicationIds] = useState<Set<string>>(() => new Set());

    const activeSteps = useMemo(() => getActiveSteps(selectedPlan), [selectedPlan]);
    const totalSteps = activeSteps.length;

    useEffect(() => {
        if (!resolvedApiId || !selectedPlan) {
            setDisabledApplicationIds(new Set());
            return;
        }

        let cancelled = false;

        void (async () => {
            const active = await getActiveSubscriptionsForApi(resolvedApiId);
            if (!cancelled) {
                setDisabledApplicationIds(
                    new Set(
                        active
                            .filter(sub => sub.plan === selectedPlan.id)
                            .map(sub => sub.application),
                    ),
                );
            }
        })();

        return () => {
            cancelled = true;
        };
    }, [resolvedApiId, selectedPlan]);

    const showApiKeyModeSelection = useMemo(() => {
        return (
            selectedPlan?.security === 'API_KEY' &&
            selectedApplication?.api_key_mode === 'UNSPECIFIED'
        );
    }, [selectedPlan, selectedApplication]);

    useEffect(() => {
        if (!resolvedApiId) {
            setLoading(false);
            return;
        }

        let cancelled = false;
        setLoading(true);

        void (async () => {
            const { getApiById } = await import('../../features/editor/services/api.service');
            const [apiData, planData] = await Promise.all([
                getApiById(resolvedApiId),
                getPlansForApi(resolvedApiId),
            ]);

            if (!cancelled) {
                setApi(apiData ?? null);
                setPlans(planData);
                setLoading(false);
            }
        })();

        return () => {
            cancelled = true;
        };
    }, [resolvedApiId]);

    useEffect(() => {
        if (currentStep !== SubscribeStep.APP_SELECTION) {
            return;
        }

        let cancelled = false;

        void (async () => {
            const response = await listApplications({
                page: applicationsPage,
                size: APPLICATIONS_PAGE_SIZE,
            });

            if (!cancelled) {
                setApplications(response.data);
                setApplicationsTotalPages(response.metadata?.pagination?.total_pages ?? 1);
                setApplicationsTotal(response.metadata?.pagination?.total ?? response.data.length);
            }
        })();

        return () => {
            cancelled = true;
        };
    }, [currentStep, applicationsPage]);

    const stepIsInvalid = useMemo(() => {
        switch (currentStep) {
            case SubscribeStep.PLAN_SELECTION:
                return selectedPlan === null;
            case SubscribeStep.APP_SELECTION:
                return (
                    selectedApplication === null ||
                    (selectedApplication !== null && disabledApplicationIds.has(selectedApplication.id))
                );
            case SubscribeStep.PUSH_DETAILS:
                return !consumerConfig.isValid;
            case SubscribeStep.REVIEW:
                if (showApiKeyModeSelection && !apiKeyMode) {
                    return true;
                }
                return false;
            default:
                return false;
        }
    }, [
        currentStep,
        selectedPlan,
        selectedApplication,
        disabledApplicationIds,
        consumerConfig.isValid,
        showApiKeyModeSelection,
        apiKeyMode,
    ]);

    const goToNextStep = useCallback(() => {
        const currentIndex = activeSteps.indexOf(currentStep);
        if (currentIndex < activeSteps.length - 1) {
            setCurrentStep(activeSteps[currentIndex + 1]);
        }
    }, [activeSteps, currentStep]);

    const goToPreviousStep = useCallback(() => {
        const currentIndex = activeSteps.indexOf(currentStep);
        if (currentIndex > 0) {
            setCurrentStep(activeSteps[currentIndex - 1]);
        }
    }, [activeSteps, currentStep]);

    const handleSelectPlan = useCallback((plan: Plan) => {
        setSelectedPlan(plan);
        setSelectedApplication(null);
        setConsumerConfig({ callbackUrl: '', isValid: false });
        setApiKeyMode(null);
        setSubscriptionError(false);
        setCompletedSubscriptionId(null);
    }, []);

    const handleSubscribe = useCallback(async () => {
        if (!resolvedApiId || !selectedPlan || subscriptionInProgress) {
            return;
        }

        if (selectedPlan.security !== 'KEY_LESS' && !selectedApplication) {
            return;
        }

        setSubscriptionInProgress(true);
        setSubscriptionError(false);

        try {
            const subscription = await createSubscription(resolvedApiId, resolvedApiName, {
                application: selectedApplication?.id ?? '',
                plan: selectedPlan.id,
                ...(apiKeyMode ? { api_key_mode: apiKeyMode } : {}),
                ...(selectedPlan.mode === 'PUSH' ? { configuration: toConsumerConfiguration(consumerConfig) } : {}),
            });
            setCompletedSubscriptionId(subscription.id);
        } catch {
            setSubscriptionError(true);
        } finally {
            setSubscriptionInProgress(false);
        }
    }, [
        resolvedApiId,
        resolvedApiName,
        selectedPlan,
        selectedApplication,
        subscriptionInProgress,
        apiKeyMode,
        consumerConfig,
    ]);

    if (!resolvedApiId) {
        return (
            <div className={styles.wrapper}>
                <div className={styles.noApiMessage}>
                    <p className={styles.emptyTitle}>No API context</p>
                    <p className={styles.emptyMessage}>
                        Place this block on a page under an API navigation item to enable subscriptions.
                    </p>
                </div>
            </div>
        );
    }

    if (loading) {
        return (
            <div className={styles.wrapper}>
                <p className={styles.loadingMessage}>Loading subscription options…</p>
            </div>
        );
    }

    if (completedSubscriptionId) {
        return (
            <div className={styles.wrapper}>
                <div className={styles.successMessage} role="status">
                    <p className={styles.successTitle}>Subscription created</p>
                    <p className={styles.emptyMessage}>
                        Your subscription <strong>{completedSubscriptionId}</strong> has been created
                        {selectedPlan?.validation === 'MANUAL' ? ' and is pending approval' : ''}.
                    </p>
                    {!isEditable && (
                        <button
                            type="button"
                            className={styles.secondaryBtn}
                            onClick={() => {
                                setCompletedSubscriptionId(null);
                                setCurrentStep(SubscribeStep.PLAN_SELECTION);
                                setSelectedPlan(null);
                                setSelectedApplication(null);
                            }}
                        >
                            Subscribe again
                        </button>
                    )}
                </div>
            </div>
        );
    }

    return (
        <div className={styles.wrapper}>
            <div className={styles.card}>
                {currentStep === SubscribeStep.PLAN_SELECTION && (
                    <>
                        <StepHeader stepNumber={stepNumberOf(SubscribeStep.PLAN_SELECTION, activeSteps)} totalSteps={totalSteps}>
                            <h2 className={styles.stepTitle}>Choose a plan</h2>
                            <p className={styles.stepDescription}>
                                A plan lets an application access an API. Once subscribed and approved, the
                                application gets credentials to use it.
                            </p>
                        </StepHeader>
                        <div className={styles.cardContent}>
                            <ChoosePlan
                                plans={plans}
                                selectedPlanId={selectedPlan?.id ?? null}
                                onSelectPlan={handleSelectPlan}
                            />
                        </div>
                    </>
                )}

                {currentStep === SubscribeStep.APP_SELECTION && (
                    <>
                        <StepHeader stepNumber={stepNumberOf(SubscribeStep.APP_SELECTION, activeSteps)} totalSteps={totalSteps}>
                            <h2 className={styles.stepTitle}>Choose an application</h2>
                            <p className={styles.stepDescription}>
                                An application represents a developer&apos;s project that interacts with the API.
                            </p>
                        </StepHeader>
                        <div className={styles.cardContent}>
                            <ChooseApplication
                                applications={applications}
                                selectedApplicationId={selectedApplication?.id ?? null}
                                disabledApplicationIds={disabledApplicationIds}
                                currentPage={applicationsPage}
                                totalPages={applicationsTotalPages}
                                totalApplications={applicationsTotal}
                                onSelectApplication={setSelectedApplication}
                                onPageChange={setApplicationsPage}
                            />
                        </div>
                    </>
                )}

                {currentStep === SubscribeStep.PUSH_DETAILS && (
                    <>
                        <StepHeader stepNumber={stepNumberOf(SubscribeStep.PUSH_DETAILS, activeSteps)} totalSteps={totalSteps}>
                            <h2 className={styles.stepTitle}>Configure consumer</h2>
                            <p className={styles.stepDescription}>
                                Set up the webhook endpoint that will receive pushed events.
                            </p>
                        </StepHeader>
                        <div className={styles.cardContent}>
                            <ConfigureConsumer value={consumerConfig} onChange={setConsumerConfig} />
                        </div>
                    </>
                )}

                {currentStep === SubscribeStep.REVIEW && selectedPlan && api && (
                    <>
                        <StepHeader stepNumber={stepNumberOf(SubscribeStep.REVIEW, activeSteps)} totalSteps={totalSteps}>
                            <h2 className={styles.stepTitle}>Review</h2>
                            {selectedPlan.security === 'KEY_LESS' && (
                                <p className={styles.stepDescription}>
                                    This plan does not need a subscription to consume the API.
                                </p>
                            )}
                        </StepHeader>
                        <div className={styles.cardContent}>
                            <ReviewAndSubscribe
                                api={api}
                                plan={selectedPlan}
                                application={selectedApplication}
                                consumerConfig={consumerConfig}
                                showApiKeyModeSelection={showApiKeyModeSelection}
                                apiKeyMode={apiKeyMode}
                                onApiKeyModeChange={setApiKeyMode}
                            />
                            {subscriptionError && (
                                <p className={styles.errorMessage} role="alert">
                                    There was an error with your subscription. Please try again.
                                </p>
                            )}
                        </div>
                    </>
                )}
            </div>

            {!isEditable && (
                <div className={styles.actions}>
                    <div>
                        {activeSteps.indexOf(currentStep) > 0 && (
                            <button
                                type="button"
                                className={styles.secondaryBtn}
                                disabled={subscriptionInProgress}
                                onClick={goToPreviousStep}
                            >
                                Previous
                            </button>
                        )}
                    </div>
                    <div className={styles.actionsRight}>
                        {selectedApplication && currentStep !== SubscribeStep.REVIEW && (
                            <span className={styles.selectedChip}>
                                {selectedApplication.name}
                                <button
                                    type="button"
                                    className={styles.chipRemove}
                                    aria-label="Clear selected application"
                                    onClick={() => setSelectedApplication(null)}
                                >
                                    ×
                                </button>
                            </span>
                        )}
                        {currentStep !== SubscribeStep.REVIEW ? (
                            <button
                                type="button"
                                className={styles.primaryBtn}
                                disabled={stepIsInvalid}
                                onClick={goToNextStep}
                            >
                                Next
                            </button>
                        ) : selectedPlan?.security !== 'KEY_LESS' ? (
                            <button
                                type="button"
                                className={styles.primaryBtn}
                                disabled={stepIsInvalid || subscriptionInProgress}
                                onClick={() => void handleSubscribe()}
                            >
                                {subscriptionInProgress ? 'Subscribing…' : 'Subscribe'}
                            </button>
                        ) : null}
                    </div>
                </div>
            )}
        </div>
    );
}
