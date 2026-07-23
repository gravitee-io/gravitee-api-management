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
import { CopyButton, isTruthyProp, maskKey } from './shared';

export function AiKeyView({
    workspaceId,
    label,
    allowRegenerate,
}: {
    workspaceId: string;
    label: string;
    allowRegenerate: boolean;
}) {
    const workspace = getAiWorkspaceData(workspaceId);
    const [revealed, setRevealed] = useState(false);

    return (
        <div className={styles.block}>
            <p className={styles.fieldLabel}>{label || 'Your AI key'}</p>
            <div className={styles.copyRow}>
                <code className={styles.codeValue}>
                    {revealed ? workspace.aiKey : maskKey(workspace.aiKey)}
                </code>
                <button type="button" className={styles.iconButton} onClick={() => setRevealed(v => !v)}>
                    {revealed ? 'Hide' : 'Show'}
                </button>
                <CopyButton value={workspace.aiKey} />
            </div>
            <p className={styles.hint}>
                Created {workspace.keyCreatedAt}. Treat this key like a password — it grants access to your
                entire AI workspace budget.
            </p>
            {allowRegenerate && (
                <div className={styles.copyRow} style={{ marginTop: 8 }}>
                    <button
                        type="button"
                        className={`${styles.iconButton} ${styles.dangerButton}`}
                        onClick={() => {
                            // POC: regeneration is illustrative only.
                            window.alert('Regenerating the AI key will immediately revoke the current key.');
                        }}
                    >
                        Regenerate key
                    </button>
                </div>
            )}
        </div>
    );
}

export const AiKeyBlock = createReactBlockSpec(
    {
        type: 'graviteeAiKey' as const,
        propSchema: {
            workspaceId: { default: '' },
            label: { default: '' },
            allowRegenerate: { default: 'true' },
        },
        content: 'none',
    },
    {
        ...getGmdBlockHooks('graviteeAiKey'),
        render: ({ block, editor }) => {
            const { workspaceId, label, allowRegenerate } = block.props;
            const view = (
                <AiKeyView
                    workspaceId={workspaceId}
                    label={label}
                    allowRegenerate={isTruthyProp(allowRegenerate)}
                />
            );

            if (!editor.isEditable) {
                return view;
            }

            const update = (key: string, value: string) =>
                editor.updateBlock(block, { props: { [key]: value } });

            return (
                <div className={`${styles.block} ${styles.editable}`}>
                    <div className={styles.editHeader}>AI Key</div>
                    <div className={styles.editGrid}>
                        <label className={styles.editField}>
                            Workspace ID
                            <input value={workspaceId} onChange={e => update('workspaceId', e.target.value)} />
                        </label>
                        <label className={styles.editField}>
                            Label
                            <input
                                value={label}
                                placeholder="Your AI key"
                                onChange={e => update('label', e.target.value)}
                            />
                        </label>
                        <label className={`${styles.editField} ${styles.checkboxField}`}>
                            <input
                                type="checkbox"
                                checked={isTruthyProp(allowRegenerate)}
                                onChange={e => update('allowRegenerate', String(e.target.checked))}
                            />
                            Allow regenerate
                        </label>
                    </div>
                    <div className={styles.editPreview}>{view}</div>
                </div>
            );
        },
    },
);
