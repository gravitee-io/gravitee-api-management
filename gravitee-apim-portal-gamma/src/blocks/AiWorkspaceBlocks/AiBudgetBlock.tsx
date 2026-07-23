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
import { createReactBlockSpec } from '@blocknote/react';

import { getAiWorkspaceData } from '../../features/editor/services/ai-workspace.service';
import { getGmdBlockHooks } from '../../features/editor/gmd/gmd-block-hooks';
import styles from './AiWorkspaceBlocks.module.scss';
import { formatCurrency, formatNumber, isTruthyProp } from './shared';

const VARIANTS = ['compact', 'detailed'] as const;

export function AiBudgetView({
    workspaceId,
    variant,
    showBreakdown,
}: {
    workspaceId: string;
    variant: string;
    showBreakdown: boolean;
}) {
    const workspace = getAiWorkspaceData(workspaceId);
    const { budget } = workspace;
    const percent = Math.min(100, Math.round((budget.used / budget.total) * 100));
    const warn = percent >= 80;
    const maxModelTokens = Math.max(...workspace.usageByModel.map(entry => entry.tokens), 1);

    return (
        <div className={styles.block}>
            {variant === 'detailed' && showBreakdown ? (
                <div className={styles.detailedBody}>
                    <div className={styles.budgetColumn}>
                        <div>
                            <p className={styles.fieldLabel}>Budget</p>
                            <div className={styles.budgetTop}>
                                <div className={styles.budgetAmount}>
                                    {formatCurrency(budget.used, budget.currency)}{' '}
                                    <span>/ {formatCurrency(budget.total, budget.currency)}</span>
                                </div>
                                <div className={styles.budgetReset}>Resets in {budget.resetInDays} days</div>
                            </div>
                            <div className={styles.progressTrack}>
                                <div
                                    className={
                                        warn
                                            ? `${styles.progressFill} ${styles.progressFillWarn}`
                                            : styles.progressFill
                                    }
                                    style={{ width: `${percent}%` }}
                                />
                            </div>
                        </div>

                        <div className={styles.metricRow}>
                            <div className={styles.metricCard}>
                                <div className={styles.metricValue}>{formatNumber(budget.tokensUsed)}</div>
                                <div className={styles.metricLabel}>Tokens used</div>
                            </div>
                            <div className={styles.metricCard}>
                                <div className={styles.metricValue}>{formatNumber(budget.requests)}</div>
                                <div className={styles.metricLabel}>Requests</div>
                            </div>
                            <div className={styles.metricCard}>
                                <div className={styles.metricValue}>
                                    {formatCurrency(budget.total - budget.used, budget.currency)}
                                </div>
                                <div className={styles.metricLabel}>Remaining</div>
                            </div>
                        </div>
                    </div>

                    <div className={styles.breakdown}>
                        <p className={styles.fieldLabel}>Spend by model</p>
                        {workspace.usageByModel.map(entry => (
                            <div key={entry.model} className={styles.breakdownRow}>
                                <span className={styles.breakdownName}>{entry.model}</span>
                                <span className={styles.breakdownBarTrack}>
                                    <span
                                        className={styles.breakdownBarFill}
                                        style={{
                                            width: `${Math.round((entry.tokens / maxModelTokens) * 100)}%`,
                                        }}
                                    />
                                </span>
                                <span className={styles.breakdownValue}>
                                    {formatCurrency(entry.cost, budget.currency)}
                                </span>
                            </div>
                        ))}
                    </div>
                </div>
            ) : (
                <>
                    <p className={styles.fieldLabel}>Budget</p>
                    <div className={styles.budgetTop}>
                        <div className={styles.budgetAmount}>
                            {formatCurrency(budget.used, budget.currency)}{' '}
                            <span>/ {formatCurrency(budget.total, budget.currency)}</span>
                        </div>
                        <div className={styles.budgetReset}>Resets in {budget.resetInDays} days</div>
                    </div>
                    <div className={styles.progressTrack}>
                        <div
                            className={
                                warn ? `${styles.progressFill} ${styles.progressFillWarn}` : styles.progressFill
                            }
                            style={{ width: `${percent}%` }}
                        />
                    </div>

                    {variant === 'detailed' && (
                        <div className={styles.metricRow}>
                            <div className={styles.metricCard}>
                                <div className={styles.metricValue}>{formatNumber(budget.tokensUsed)}</div>
                                <div className={styles.metricLabel}>Tokens used</div>
                            </div>
                            <div className={styles.metricCard}>
                                <div className={styles.metricValue}>{formatNumber(budget.requests)}</div>
                                <div className={styles.metricLabel}>Requests</div>
                            </div>
                            <div className={styles.metricCard}>
                                <div className={styles.metricValue}>
                                    {formatCurrency(budget.total - budget.used, budget.currency)}
                                </div>
                                <div className={styles.metricLabel}>Remaining</div>
                            </div>
                        </div>
                    )}
                </>
            )}
        </div>
    );
}

export const AiBudgetBlock = createReactBlockSpec(
    {
        type: 'graviteeAiBudget' as const,
        propSchema: {
            workspaceId: { default: '' },
            variant: { default: 'compact' },
            showBreakdown: { default: 'true' },
        },
        content: 'none',
    },
    {
        ...getGmdBlockHooks('graviteeAiBudget'),
        render: ({ block, editor }) => {
            const { workspaceId, variant, showBreakdown } = block.props;
            const view = (
                <AiBudgetView
                    workspaceId={workspaceId}
                    variant={variant}
                    showBreakdown={isTruthyProp(showBreakdown)}
                />
            );

            if (!editor.isEditable) {
                return view;
            }

            const update = (key: string, value: string) =>
                editor.updateBlock(block, { props: { [key]: value } });

            return (
                <div className={`${styles.block} ${styles.editable}`}>
                    <div className={styles.editHeader}>AI Budget</div>
                    <div className={styles.editGrid}>
                        <label className={styles.editField}>
                            Workspace ID
                            <input value={workspaceId} onChange={e => update('workspaceId', e.target.value)} />
                        </label>
                        <label className={styles.editField}>
                            Variant
                            <select value={variant} onChange={e => update('variant', e.target.value)}>
                                {VARIANTS.map(option => (
                                    <option key={option} value={option}>
                                        {option}
                                    </option>
                                ))}
                            </select>
                        </label>
                        <label className={`${styles.editField} ${styles.checkboxField}`}>
                            <input
                                type="checkbox"
                                checked={isTruthyProp(showBreakdown)}
                                onChange={e => update('showBreakdown', String(e.target.checked))}
                            />
                            Show per-model breakdown
                        </label>
                    </div>
                    <div className={styles.editPreview}>{view}</div>
                </div>
            );
        },
    },
);
