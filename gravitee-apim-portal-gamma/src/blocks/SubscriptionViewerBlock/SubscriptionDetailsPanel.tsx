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
import { useCallback, useEffect, useState } from 'react';

import type { Api } from '../../features/editor/entities/api';
import type { Subscription } from '../../features/editor/entities/subscription';
import { getPlanSecurityLabel } from '../../features/editor/entities/plan';
import { getApiById } from '../../features/editor/services/api.service';
import { renewApiKey, revokeApiKey } from '../../features/editor/services/subscriptions.service';
import { formatDate, formatStatus, isActiveApiKey } from './utils';

import styles from './SubscriptionViewerBlock.module.scss';

interface SubscriptionDetailsPanelProps {
    readonly subscription: Subscription;
    readonly onClose: () => void;
    readonly onUpdated: (subscription: Subscription) => void;
}

export function SubscriptionDetailsPanel({
    subscription,
    onClose,
    onUpdated,
}: SubscriptionDetailsPanelProps) {
    const [api, setApi] = useState<Api | null>(null);
    const [actionInProgress, setActionInProgress] = useState(false);
    const [feedback, setFeedback] = useState<string | null>(null);

    useEffect(() => {
        let cancelled = false;
        void (async () => {
            const apiData = await getApiById(subscription.api);
            if (!cancelled) setApi(apiData ?? null);
        })();
        return () => {
            cancelled = true;
        };
    }, [subscription.api]);

    useEffect(() => {
        const handleEscape = (event: KeyboardEvent) => {
            if (event.key === 'Escape') onClose();
        };
        document.addEventListener('keydown', handleEscape);
        return () => document.removeEventListener('keydown', handleEscape);
    }, [onClose]);

    const handleRevokeKey = useCallback(
        async (keyId: string) => {
            setActionInProgress(true);
            setFeedback(null);
            try {
                const updated = await revokeApiKey(subscription.id, keyId);
                onUpdated(updated);
                setFeedback('API key revoked.');
            } catch {
                setFeedback('Failed to revoke API key.');
            } finally {
                setActionInProgress(false);
            }
        },
        [subscription.id, onUpdated],
    );

    const handleRenewKey = useCallback(async () => {
        setActionInProgress(true);
        setFeedback(null);
        try {
            const updated = await renewApiKey(subscription.id);
            onUpdated(updated);
            setFeedback('New API key generated.');
        } catch {
            setFeedback('Failed to renew API key.');
        } finally {
            setActionInProgress(false);
        }
    }, [subscription.id, onUpdated]);

    const showApiAccess =
        subscription.status === 'ACCEPTED' || subscription.security === 'KEY_LESS';

    return (
        <>
            <button
                type="button"
                className={styles.overlay}
                aria-label="Close subscription details"
                onClick={onClose}
            />
            <aside
                className={styles.slideOver}
                role="dialog"
                aria-modal="true"
                aria-labelledby="subscription-details-title"
            >
                <header className={styles.panelHeader}>
                    <h2 id="subscription-details-title" className={styles.panelTitle}>
                        Subscription Details
                    </h2>
                    <button type="button" className={styles.closeBtn} onClick={onClose} aria-label="Close">
                        ×
                    </button>
                </header>

                <div className={styles.panelBody}>
                    <section className={styles.detailSection} aria-label="Subscription information">
                        <h3 className={styles.sectionTitle}>Subscription info</h3>
                        <dl className={styles.detailList}>
                            <div className={styles.detailRow}>
                                <dt>ID</dt>
                                <dd>{subscription.id}</dd>
                            </div>
                            <div className={styles.detailRow}>
                                <dt>Status</dt>
                                <dd>{formatStatus(subscription.status)}</dd>
                            </div>
                            <div className={styles.detailRow}>
                                <dt>API</dt>
                                <dd>{subscription.apiName ?? subscription.api}</dd>
                            </div>
                            <div className={styles.detailRow}>
                                <dt>Plan</dt>
                                <dd>{subscription.planName ?? subscription.plan}</dd>
                            </div>
                            <div className={styles.detailRow}>
                                <dt>Application</dt>
                                <dd>{subscription.applicationName ?? subscription.application}</dd>
                            </div>
                            {subscription.security && (
                                <div className={styles.detailRow}>
                                    <dt>Authentication</dt>
                                    <dd>{getPlanSecurityLabel(subscription.security)}</dd>
                                </div>
                            )}
                            {subscription.created_at && (
                                <div className={styles.detailRow}>
                                    <dt>Created</dt>
                                    <dd>{formatDate(subscription.created_at)}</dd>
                                </div>
                            )}
                        </dl>
                    </section>

                    {showApiAccess && (
                        <section className={styles.detailSection} aria-label="API access">
                            <div className={styles.sectionHeader}>
                                <h3 className={styles.sectionTitle}>API access</h3>
                                {subscription.security === 'API_KEY' &&
                                    subscription.status === 'ACCEPTED' && (
                                        <button
                                            type="button"
                                            className={styles.secondaryBtn}
                                            disabled={actionInProgress}
                                            onClick={() => void handleRenewKey()}
                                        >
                                            Renew API Key
                                        </button>
                                    )}
                            </div>

                            {subscription.status === 'PENDING' && subscription.security !== 'KEY_LESS' && (
                                <p className={styles.hint}>
                                    Your subscription request is being validated. Come back later.
                                </p>
                            )}

                            {subscription.security === 'API_KEY' && subscription.keys && (
                                <ul className={styles.keyList}>
                                    {subscription.keys.map(key => (
                                        <li key={key.id ?? key.key} className={styles.keyItem}>
                                            <code className={styles.keyValue}>{key.key ?? '—'}</code>
                                            <span className={styles.keyMeta}>
                                                {key.revoked_at
                                                    ? 'Revoked'
                                                    : isActiveApiKey(key)
                                                      ? 'Active'
                                                      : 'Expired'}
                                            </span>
                                            {isActiveApiKey(key) && key.id && subscription.status === 'ACCEPTED' && (
                                                <button
                                                    type="button"
                                                    className={styles.linkBtn}
                                                    disabled={actionInProgress}
                                                    onClick={() => void handleRevokeKey(key.id!)}
                                                >
                                                    Revoke
                                                </button>
                                            )}
                                        </li>
                                    ))}
                                </ul>
                            )}

                            {(subscription.security === 'OAUTH2' || subscription.security === 'JWT') && (
                                <dl className={styles.detailList}>
                                    {subscription.clientId && (
                                        <div className={styles.detailRow}>
                                            <dt>Client ID</dt>
                                            <dd>
                                                <code>{subscription.clientId}</code>
                                            </dd>
                                        </div>
                                    )}
                                    {subscription.clientSecret && (
                                        <div className={styles.detailRow}>
                                            <dt>Client Secret</dt>
                                            <dd>
                                                <code>{subscription.clientSecret}</code>
                                            </dd>
                                        </div>
                                    )}
                                </dl>
                            )}

                            {api && api.entrypoints.length > 0 && (
                                <div className={styles.entrypoints}>
                                    <p className={styles.hint}>Entrypoints</p>
                                    <ul>
                                        {api.entrypoints.map(entrypoint => (
                                            <li key={entrypoint}>
                                                <code>{entrypoint}</code>
                                            </li>
                                        ))}
                                    </ul>
                                </div>
                            )}
                        </section>
                    )}

                    {!showApiAccess && subscription.status !== 'ACCEPTED' && (
                        <section className={styles.detailSection}>
                            <p className={styles.hint}>
                                API access credentials are not available for {formatStatus(subscription.status)}{' '}
                                subscriptions.
                            </p>
                        </section>
                    )}

                    {subscription.consumerConfiguration && (
                        <section className={styles.detailSection} aria-label="Webhook configuration">
                            <h3 className={styles.sectionTitle}>Webhook configuration</h3>
                            <dl className={styles.detailList}>
                                <div className={styles.detailRow}>
                                    <dt>Callback URL</dt>
                                    <dd>
                                        {subscription.consumerConfiguration.entrypointConfiguration.callbackUrl}
                                    </dd>
                                </div>
                                <div className={styles.detailRow}>
                                    <dt>Channel</dt>
                                    <dd>{subscription.consumerConfiguration.channel}</dd>
                                </div>
                                <div className={styles.detailRow}>
                                    <dt>Retry</dt>
                                    <dd>
                                        {
                                            subscription.consumerConfiguration.entrypointConfiguration.retry
                                                .retryOption
                                        }
                                    </dd>
                                </div>
                            </dl>
                        </section>
                    )}

                    {feedback && (
                        <p className={styles.feedback} role="status">
                            {feedback}
                        </p>
                    )}
                </div>
            </aside>
        </>
    );
}
