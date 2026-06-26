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
import type { Plan } from '../../../features/editor/entities/plan';
import { getPlanSecurityLabel } from '../../../features/editor/entities/plan';

import styles from '../SubscriptionFlowBlock.module.scss';

interface ChoosePlanProps {
    readonly plans: Plan[];
    readonly selectedPlanId: string | null;
    readonly onSelectPlan: (plan: Plan) => void;
}

function formatUsageLimit(limit: number | undefined, unit: string | undefined): string | null {
    if (!limit) return null;
    const unitLabel = unit?.toLowerCase() ?? 'period';
    return `${limit} hits / ${unitLabel}`;
}

export function ChoosePlan({ plans, selectedPlanId, onSelectPlan }: ChoosePlanProps) {
    if (plans.length === 0) {
        return <p className={styles.emptyMessage}>No plans available for this API.</p>;
    }

    return (
        <div className={styles.planList} role="radiogroup" aria-label="Choose a plan">
            {plans.map(plan => {
                const selected = plan.id === selectedPlanId;
                const rateLimit = formatUsageLimit(
                    plan.usage_configuration?.rate_limit?.limit,
                    plan.usage_configuration?.rate_limit?.period_time_unit,
                );
                const quota = formatUsageLimit(
                    plan.usage_configuration?.quota?.limit,
                    plan.usage_configuration?.quota?.period_time_unit,
                );

                return (
                    <button
                        key={plan.id}
                        type="button"
                        role="radio"
                        aria-checked={selected}
                        className={`${styles.radioCard} ${selected ? styles.radioCardSelected : ''}`}
                        onClick={() => onSelectPlan(plan)}
                    >
                        <div className={styles.radioCardHeader}>
                            <span className={styles.radioIndicator} aria-hidden="true" />
                            <span className={styles.radioCardTitle}>{plan.name}</span>
                        </div>
                        {plan.description && <p className={styles.radioCardDescription}>{plan.description}</p>}
                        <div className={styles.badges}>
                            <span className={styles.badge}>{getPlanSecurityLabel(plan.security)}</span>
                            <span className={styles.badge}>
                                {plan.validation === 'AUTO' ? 'No approval' : 'Requires approval'}
                            </span>
                            {plan.mode === 'PUSH' && <span className={styles.badge}>Push / Webhook</span>}
                            {rateLimit && <span className={styles.badge}>Rate limit: {rateLimit}</span>}
                            {quota && <span className={styles.badge}>Quota: {quota}</span>}
                        </div>
                    </button>
                );
            })}
        </div>
    );
}
