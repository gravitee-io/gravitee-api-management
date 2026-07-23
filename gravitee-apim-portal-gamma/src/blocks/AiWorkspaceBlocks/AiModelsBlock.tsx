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

export function AiModelsView({ workspaceId }: { workspaceId: string }) {
    const workspace = getAiWorkspaceData(workspaceId);

    return (
        <div className={styles.block}>
            <div className={styles.modelList}>
                <div className={styles.modelHeader} role="row">
                    <span>Model</span>
                    <span>Capabilities</span>
                    <span>Context</span>
                    <span>Price</span>
                </div>
                {workspace.models.map(model => (
                    <div key={model.id} className={styles.modelRow} role="row">
                        <div className={styles.modelIdentity}>
                            <div className={styles.modelAvatar} aria-hidden>
                                {model.provider.slice(0, 1)}
                            </div>
                            <div>
                                <div className={styles.modelName}>{model.name}</div>
                                <div className={styles.modelProvider}>{model.provider}</div>
                            </div>
                        </div>
                        <div className={styles.capRow}>
                            {model.capabilities.map(capability => (
                                <span key={capability} className={styles.capChip}>
                                    {capability}
                                </span>
                            ))}
                        </div>
                        <span className={styles.modelMetaPill}>{model.contextWindow}</span>
                        <span className={styles.modelTier}>{model.tier}</span>
                    </div>
                ))}
            </div>
        </div>
    );
}

export const AiModelsBlock = createReactBlockSpec(
    {
        type: 'graviteeAiModels' as const,
        propSchema: {
            workspaceId: { default: '' },
        },
        content: 'none',
    },
    {
        ...getGmdBlockHooks('graviteeAiModels'),
        render: ({ block, editor }) => {
            const { workspaceId } = block.props;
            const view = <AiModelsView workspaceId={workspaceId} />;

            if (!editor.isEditable) {
                return view;
            }

            const update = (key: string, value: string) =>
                editor.updateBlock(block, { props: { [key]: value } });

            return (
                <div className={`${styles.block} ${styles.editable}`}>
                    <div className={styles.editHeader}>AI Models</div>
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
