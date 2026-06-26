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
import type { Api } from '../../../features/editor/entities/api';
import type { Application } from '../../../features/editor/entities/application';
import type { Plan } from '../../../features/editor/entities/plan';
import { getPlanSecurityLabel } from '../../../features/editor/entities/plan';
import type { ConsumerFormState } from '../types';

import styles from '../SubscriptionFlowBlock.module.scss';

interface ReviewAndSubscribeProps {
    readonly api: Api;
    readonly plan: Plan;
    readonly application: Application | null;
    readonly consumerConfig: ConsumerFormState;
    readonly showApiKeyModeSelection: boolean;
    readonly apiKeyMode: 'EXCLUSIVE' | 'SHARED' | null;
    readonly onApiKeyModeChange: (mode: 'EXCLUSIVE' | 'SHARED') => void;
}

export function ReviewAndSubscribe({
    api,
    plan,
    application,
    consumerConfig,
    showApiKeyModeSelection,
    apiKeyMode,
    onApiKeyModeChange,
}: ReviewAndSubscribeProps) {
    return (
        <div className={styles.review}>
            <section className={styles.reviewSection} aria-label="Subscription summary">
                <h3 className={styles.reviewSectionTitle}>Subscription details</h3>
                <dl className={styles.reviewList}>
                    <div className={styles.reviewRow}>
                        <dt>API</dt>
                        <dd>{api.name}</dd>
                    </div>
                    <div className={styles.reviewRow}>
                        <dt>Plan</dt>
                        <dd>{plan.name}</dd>
                    </div>
                    <div className={styles.reviewRow}>
                        <dt>Security</dt>
                        <dd>{getPlanSecurityLabel(plan.security)}</dd>
                    </div>
                    <div className={styles.reviewRow}>
                        <dt>Approval</dt>
                        <dd>{plan.validation === 'AUTO' ? 'Automatic' : 'Manual review required'}</dd>
                    </div>
                    {application && (
                        <div className={styles.reviewRow}>
                            <dt>Application</dt>
                            <dd>{application.name}</dd>
                        </div>
                    )}
                    {plan.mode === 'PUSH' && consumerConfig.callbackUrl && (
                        <div className={styles.reviewRow}>
                            <dt>Webhook URL</dt>
                            <dd>{consumerConfig.callbackUrl}</dd>
                        </div>
                    )}
                </dl>
            </section>

            {plan.security === 'KEY_LESS' && (
                <section className={styles.reviewSection} aria-label="API access">
                    <h3 className={styles.reviewSectionTitle}>API access</h3>
                    <p className={styles.fieldHint}>
                        This plan does not require a subscription. Use the entrypoints below to consume the API.
                    </p>
                    <ul className={styles.entrypointList}>
                        {api.entrypoints.map(entrypoint => (
                            <li key={entrypoint}>
                                <code>{entrypoint}</code>
                            </li>
                        ))}
                    </ul>
                </section>
            )}

            {showApiKeyModeSelection && (
                <section className={styles.reviewSection} aria-label="API key mode">
                    <h3 className={styles.reviewSectionTitle}>Choose API key management mode</h3>
                    <p className={styles.fieldHint}>
                        This decision is final and cannot be changed after subscribing.
                    </p>
                    <div className={styles.apiKeyModeList} role="radiogroup" aria-label="API key mode">
                        <button
                            type="button"
                            role="radio"
                            aria-checked={apiKeyMode === 'EXCLUSIVE'}
                            className={`${styles.radioCard} ${apiKeyMode === 'EXCLUSIVE' ? styles.radioCardSelected : ''}`}
                            onClick={() => onApiKeyModeChange('EXCLUSIVE')}
                        >
                            <span className={styles.radioCardTitle}>Generated API Key</span>
                            <p className={styles.radioCardDescription}>
                                A new API key is generated for each subscription of this application.
                            </p>
                        </button>
                        <button
                            type="button"
                            role="radio"
                            aria-checked={apiKeyMode === 'SHARED'}
                            className={`${styles.radioCard} ${apiKeyMode === 'SHARED' ? styles.radioCardSelected : ''}`}
                            onClick={() => onApiKeyModeChange('SHARED')}
                        >
                            <span className={styles.radioCardTitle}>Shared API Key</span>
                            <p className={styles.radioCardDescription}>
                                The same API key is reused across subscriptions for simpler management.
                            </p>
                        </button>
                    </div>
                </section>
            )}
        </div>
    );
}
