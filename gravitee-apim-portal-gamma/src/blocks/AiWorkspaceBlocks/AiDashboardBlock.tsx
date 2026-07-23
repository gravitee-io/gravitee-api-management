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
import { AiModelsView } from './AiModelsBlock';
import { AiUsageHistoryView } from './AiUsageHistoryBlock';
import styles from './AiWorkspaceBlocks.module.scss';

function AiDashboardView({ workspaceId }: { workspaceId: string }) {
    return (
        <div className={styles.dashboardLayout}>
            <div className={styles.dashboardLeft}>
                <div className={styles.groupSection}>
                    <h3 className={styles.groupTitle}>Budget</h3>
                    <AiBudgetView workspaceId={workspaceId} variant="compact" showBreakdown={false} />
                </div>
                <div className={styles.groupSection}>
                    <h3 className={styles.groupTitle}>Available models</h3>
                    <AiModelsView workspaceId={workspaceId} />
                </div>
            </div>
            <div className={styles.groupSection}>
                <h3 className={styles.groupTitle}>Usage</h3>
                <AiUsageHistoryView key={`${workspaceId}-7`} workspaceId={workspaceId} defaultRange={7} />
            </div>
        </div>
    );
}

export const AiDashboardBlock = createReactBlockSpec(
    {
        type: 'graviteeAiDashboard' as const,
        propSchema: {
            workspaceId: { default: '' },
        },
        content: 'none',
    },
    {
        ...getGmdBlockHooks('graviteeAiDashboard'),
        render: ({ block, editor }) => {
            const { workspaceId } = block.props;
            const view = <AiDashboardView workspaceId={workspaceId} />;

            if (!editor.isEditable) {
                return view;
            }

            const update = (key: string, value: string) =>
                editor.updateBlock(block, { props: { [key]: value } });

            return (
                <div className={`${styles.block} ${styles.editable}`}>
                    <div className={styles.editHeader}>AI Overview Dashboard</div>
                    <div className={styles.editGrid}>
                        <label className={styles.editField}>
                            Workspace ID
                            <input value={workspaceId} onChange={e => update('workspaceId', e.target.value)} />
                        </label>
                    </div>
                    <div className={styles.editPreview}>{view}</div>
                </div>
            );
        },
    },
);
