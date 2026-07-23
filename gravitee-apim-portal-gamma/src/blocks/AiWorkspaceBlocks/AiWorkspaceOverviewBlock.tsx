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
import { isTruthyProp } from './shared';

function AiWorkspaceOverviewView({
    workspaceId,
    showProviders,
    showStatus,
}: {
    workspaceId: string;
    showProviders: boolean;
    showStatus: boolean;
}) {
    const workspace = getAiWorkspaceData(workspaceId);

    return (
        <div className={styles.overview}>
            <div className={styles.overviewTop}>
                <h2 className={styles.overviewName}>{workspace.name}</h2>
                {showStatus && (
                    <span className={styles.statusBadge}>
                        <span
                            className={
                                workspace.status === 'active'
                                    ? styles.statusDot
                                    : `${styles.statusDot} ${styles.statusDotSuspended}`
                            }
                        />
                        {workspace.status}
                    </span>
                )}
            </div>
            <p className={styles.overviewDesc}>{workspace.description}</p>
            {showProviders && workspace.providers.length > 0 && (
                <div className={styles.providerRow}>
                    {workspace.providers.map(provider => (
                        <span key={provider} className={styles.providerChip}>
                            {provider}
                        </span>
                    ))}
                </div>
            )}
        </div>
    );
}

export const AiWorkspaceOverviewBlock = createReactBlockSpec(
    {
        type: 'graviteeAiWorkspaceOverview' as const,
        propSchema: {
            workspaceId: { default: '' },
            showProviders: { default: 'true' },
            showStatus: { default: 'true' },
        },
        content: 'none',
    },
    {
        ...getGmdBlockHooks('graviteeAiWorkspaceOverview'),
        render: ({ block, editor }) => {
            const { workspaceId, showProviders, showStatus } = block.props;
            const workspace = getAiWorkspaceData(workspaceId);
            const view = (
                <AiWorkspaceOverviewView
                    workspaceId={workspaceId}
                    showProviders={isTruthyProp(showProviders)}
                    showStatus={isTruthyProp(showStatus)}
                />
            );

            if (!editor.isEditable) {
                return view;
            }

            const update = (key: string, value: string) =>
                editor.updateBlock(block, { props: { [key]: value } });

            return (
                <div className={`${styles.block} ${styles.editable}`}>
                    <div className={styles.editHeader}>AI Workspace Overview</div>
                    <div className={styles.editGrid}>
                        <label className={styles.editField}>
                            Workspace ID
                            <input
                                value={workspaceId}
                                placeholder={workspace.id}
                                onChange={e => update('workspaceId', e.target.value)}
                            />
                        </label>
                        <label className={`${styles.editField} ${styles.checkboxField}`}>
                            <input
                                type="checkbox"
                                checked={isTruthyProp(showStatus)}
                                onChange={e => update('showStatus', String(e.target.checked))}
                            />
                            Show status badge
                        </label>
                        <label className={`${styles.editField} ${styles.checkboxField}`}>
                            <input
                                type="checkbox"
                                checked={isTruthyProp(showProviders)}
                                onChange={e => update('showProviders', String(e.target.checked))}
                            />
                            Show providers
                        </label>
                    </div>
                    <div className={styles.editPreview}>{view}</div>
                </div>
            );
        },
    },
);
