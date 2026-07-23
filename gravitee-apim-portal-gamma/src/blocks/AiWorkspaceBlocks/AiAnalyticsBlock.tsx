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

import { getGmdBlockHooks } from '../../features/editor/gmd/gmd-block-hooks';
import { AiBudgetView } from './AiBudgetBlock';
import { AiUsageHistoryView } from './AiUsageHistoryBlock';
import styles from './AiWorkspaceBlocks.module.scss';

const RANGE_OPTIONS = [7, 14] as const;

function AiAnalyticsView({ workspaceId, range }: { workspaceId: string; range: number }) {
    return (
        <div className={styles.groupStack}>
            <div className={styles.groupSection}>
                <h3 className={styles.groupTitle}>Budget details</h3>
                <AiBudgetView workspaceId={workspaceId} variant="detailed" showBreakdown />
            </div>
            <div className={styles.groupSection}>
                <h3 className={styles.groupTitle}>Usage over time</h3>
                <AiUsageHistoryView
                    key={`${workspaceId}-${range}`}
                    workspaceId={workspaceId}
                    defaultRange={range}
                />
            </div>
        </div>
    );
}

export const AiAnalyticsBlock = createReactBlockSpec(
    {
        type: 'graviteeAiAnalytics' as const,
        propSchema: {
            workspaceId: { default: '' },
            range: { default: '14' },
        },
        content: 'none',
    },
    {
        ...getGmdBlockHooks('graviteeAiAnalytics'),
        render: ({ block, editor }) => {
            const { workspaceId, range } = block.props;
            const parsedRange = Number.parseInt(range, 10) || 14;
            const view = <AiAnalyticsView workspaceId={workspaceId} range={parsedRange} />;

            if (!editor.isEditable) {
                return view;
            }

            const update = (key: string, value: string) =>
                editor.updateBlock(block, { props: { [key]: value } });

            return (
                <div className={`${styles.block} ${styles.editable}`}>
                    <div className={styles.editHeader}>AI Budget & Usage Analytics</div>
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
