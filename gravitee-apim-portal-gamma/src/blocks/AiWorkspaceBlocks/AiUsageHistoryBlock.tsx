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
import { useState } from 'react';

import { getAiWorkspaceData } from '../../features/editor/services/ai-workspace.service';
import { getGmdBlockHooks } from '../../features/editor/gmd/gmd-block-hooks';
import styles from './AiWorkspaceBlocks.module.scss';
import { formatCurrency, formatNumber } from './shared';

type Metric = 'tokens' | 'requests' | 'cost';

const RANGE_OPTIONS = [7, 14] as const;
const METRIC_LABELS: Record<Metric, string> = {
    tokens: 'Tokens',
    requests: 'Requests',
    cost: 'Cost',
};

export function AiUsageHistoryView({ workspaceId, defaultRange }: { workspaceId: string; defaultRange: number }) {
    const workspace = getAiWorkspaceData(workspaceId);
    const [range, setRange] = useState<number>(defaultRange);
    const [metric, setMetric] = useState<Metric>('tokens');

    const entries = workspace.usageByDay.slice(-range);
    const max = Math.max(...entries.map(entry => entry[metric]), 1);

    const formatValue = (value: number): string =>
        metric === 'cost' ? formatCurrency(value, workspace.budget.currency) : formatNumber(value);

    return (
        <div className={styles.block}>
            <div className={styles.rangeRow}>
                <div className={styles.rangeButtons}>
                    {(Object.keys(METRIC_LABELS) as Metric[]).map(option => (
                        <button
                            key={option}
                            type="button"
                            className={option === metric ? styles.tabActive : styles.tab}
                            onClick={() => setMetric(option)}
                        >
                            {METRIC_LABELS[option]}
                        </button>
                    ))}
                </div>
                <div className={styles.rangeButtons}>
                    {RANGE_OPTIONS.map(option => (
                        <button
                            key={option}
                            type="button"
                            className={option === range ? styles.tabActive : styles.tab}
                            onClick={() => setRange(option)}
                        >
                            {option}d
                        </button>
                    ))}
                </div>
            </div>

            <div className={styles.chart}>
                {entries.map(entry => (
                    <div
                        key={entry.date}
                        className={styles.chartBar}
                        style={{ height: `${Math.max(4, Math.round((entry[metric] / max) * 100))}%` }}
                        title={`${entry.date}: ${formatValue(entry[metric])}`}
                    />
                ))}
            </div>
            <div className={styles.chartLabels}>
                {entries.map(entry => (
                    <span key={entry.date} className={styles.chartLabel}>
                        {entry.date.slice(5)}
                    </span>
                ))}
            </div>

            <table className={styles.table} style={{ marginTop: 18 }}>
                <thead>
                    <tr>
                        <th>Date</th>
                        <th>Tokens</th>
                        <th>Requests</th>
                        <th>Cost</th>
                    </tr>
                </thead>
                <tbody>
                    {[...entries].reverse().slice(0, 7).map(entry => (
                        <tr key={entry.date}>
                            <td>{entry.date}</td>
                            <td>{formatNumber(entry.tokens)}</td>
                            <td>{formatNumber(entry.requests)}</td>
                            <td>{formatCurrency(entry.cost, workspace.budget.currency)}</td>
                        </tr>
                    ))}
                </tbody>
            </table>
        </div>
    );
}

export const AiUsageHistoryBlock = createReactBlockSpec(
    {
        type: 'graviteeAiUsageHistory' as const,
        propSchema: {
            workspaceId: { default: '' },
            range: { default: '14' },
        },
        content: 'none',
    },
    {
        ...getGmdBlockHooks('graviteeAiUsageHistory'),
        render: ({ block, editor }) => {
            const { workspaceId, range } = block.props;
            const parsedRange = Number.parseInt(range, 10) || 14;
            // Remount when settings change so the preview picks up a new default range.
            const view = (
                <AiUsageHistoryView
                    key={`${workspaceId}-${parsedRange}`}
                    workspaceId={workspaceId}
                    defaultRange={parsedRange}
                />
            );

            if (!editor.isEditable) {
                return view;
            }

            const update = (key: string, value: string) =>
                editor.updateBlock(block, { props: { [key]: value } });

            return (
                <div className={`${styles.block} ${styles.editable}`}>
                    <div className={styles.editHeader}>AI Usage History</div>
                    <div className={styles.editGrid}>
                        <label className={styles.editField}>
                            Workspace ID
                            <input value={workspaceId} onChange={e => update('workspaceId', e.target.value)} />
                        </label>
                        <label className={styles.editField}>
                            Default range (days)
                            <select value={range} onChange={e => update('range', e.target.value)}>
                                {RANGE_OPTIONS.map(option => (
                                    <option key={option} value={String(option)}>
                                        {option}
                                    </option>
                                ))}
                            </select>
                        </label>
                    </div>
                    <div className={styles.editPreview}>{view}</div>
                </div>
            );
        },
    },
);
