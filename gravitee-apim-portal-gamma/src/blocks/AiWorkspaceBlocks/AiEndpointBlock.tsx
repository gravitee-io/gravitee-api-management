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
import { CopyButton } from './shared';

export function AiEndpointView({
    workspaceId,
    label,
    urlOverride,
}: {
    workspaceId: string;
    label: string;
    urlOverride: string;
}) {
    const workspace = getAiWorkspaceData(workspaceId);
    const endpoint = urlOverride || workspace.endpoint;

    return (
        <div className={styles.block}>
            <p className={styles.fieldLabel}>{label || 'Gateway endpoint'}</p>
            <div className={styles.copyRow}>
                <code className={styles.codeValue}>{endpoint}</code>
                <CopyButton value={endpoint} />
            </div>
            <p className={styles.headerLine}>
                Send requests with the header{' '}
                <code>{workspace.headerName}: Bearer &lt;your-ai-key&gt;</code>. The endpoint is
                OpenAI-compatible — point any OpenAI SDK at it by setting the base URL.
            </p>
        </div>
    );
}

export const AiEndpointBlock = createReactBlockSpec(
    {
        type: 'graviteeAiEndpoint' as const,
        propSchema: {
            workspaceId: { default: '' },
            label: { default: '' },
            urlOverride: { default: '' },
        },
        content: 'none',
    },
    {
        ...getGmdBlockHooks('graviteeAiEndpoint'),
        render: ({ block, editor }) => {
            const { workspaceId, label, urlOverride } = block.props;
            const workspace = getAiWorkspaceData(workspaceId);
            const view = (
                <AiEndpointView workspaceId={workspaceId} label={label} urlOverride={urlOverride} />
            );

            if (!editor.isEditable) {
                return view;
            }

            const update = (key: string, value: string) =>
                editor.updateBlock(block, { props: { [key]: value } });

            return (
                <div className={`${styles.block} ${styles.editable}`}>
                    <div className={styles.editHeader}>AI Endpoint</div>
                    <div className={styles.editGrid}>
                        <label className={styles.editField}>
                            Workspace ID
                            <input value={workspaceId} onChange={e => update('workspaceId', e.target.value)} />
                        </label>
                        <label className={styles.editField}>
                            Label
                            <input
                                value={label}
                                placeholder="Gateway endpoint"
                                onChange={e => update('label', e.target.value)}
                            />
                        </label>
                        <label className={styles.editField}>
                            URL override
                            <input
                                value={urlOverride}
                                placeholder={workspace.endpoint}
                                onChange={e => update('urlOverride', e.target.value)}
                            />
                        </label>
                    </div>
                    <div className={styles.editPreview}>{view}</div>
                </div>
            );
        },
    },
);
