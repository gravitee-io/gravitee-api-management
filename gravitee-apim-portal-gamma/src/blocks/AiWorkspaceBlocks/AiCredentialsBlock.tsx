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
import { AiEndpointView } from './AiEndpointBlock';
import { AiKeyView } from './AiKeyBlock';
import { AiSnippetsView } from './AiSnippetsBlock';
import styles from './AiWorkspaceBlocks.module.scss';
import { isTruthyProp } from './shared';

function AiCredentialsView({
    workspaceId,
    languages,
    useRealKey,
}: {
    workspaceId: string;
    languages: string;
    useRealKey: boolean;
}) {
    return (
        <div className={styles.groupStack}>
            <div className={styles.groupSection}>
                <h3 className={styles.groupTitle}>Credentials</h3>
                <div className={styles.groupSplit}>
                    <AiKeyView workspaceId={workspaceId} label="Your AI key" allowRegenerate />
                    <AiEndpointView workspaceId={workspaceId} label="Gateway endpoint" urlOverride="" />
                </div>
            </div>
            <div className={styles.groupSection}>
                <h3 className={styles.groupTitle}>Code snippets</h3>
                <AiSnippetsView
                    workspaceId={workspaceId}
                    languagesRaw={languages}
                    useRealKey={useRealKey}
                />
            </div>
        </div>
    );
}

export const AiCredentialsBlock = createReactBlockSpec(
    {
        type: 'graviteeAiCredentials' as const,
        propSchema: {
            workspaceId: { default: '' },
            languages: { default: '' },
            useRealKey: { default: 'false' },
        },
        content: 'none',
    },
    {
        ...getGmdBlockHooks('graviteeAiCredentials'),
        render: ({ block, editor }) => {
            const { workspaceId, languages, useRealKey } = block.props;
            const view = (
                <AiCredentialsView
                    workspaceId={workspaceId}
                    languages={languages}
                    useRealKey={isTruthyProp(useRealKey, false)}
                />
            );

            if (!editor.isEditable) {
                return view;
            }

            const update = (key: string, value: string) =>
                editor.updateBlock(block, { props: { [key]: value } });

            return (
                <div className={`${styles.block} ${styles.editable}`}>
                    <div className={styles.editHeader}>AI Credentials & Snippets</div>
                    <div className={styles.editGrid}>
                        <label className={styles.editField}>
                            Workspace ID
                            <input value={workspaceId} onChange={e => update('workspaceId', e.target.value)} />
                        </label>
                        <label className={styles.editField}>
                            Languages (comma-separated)
                            <input
                                value={languages}
                                placeholder="curl, python, javascript, java, go"
                                onChange={e => update('languages', e.target.value)}
                            />
                        </label>
                        <label className={`${styles.editField} ${styles.checkboxField}`}>
                            <input
                                type="checkbox"
                                checked={isTruthyProp(useRealKey, false)}
                                onChange={e => update('useRealKey', String(e.target.checked))}
                            />
                            Embed real AI key (instead of placeholder)
                        </label>
                    </div>
                    <div className={styles.editPreview}>{view}</div>
                </div>
            );
        },
    },
);
