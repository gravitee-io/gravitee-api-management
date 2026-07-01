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

import { useApiData } from './ApiDataContext';
import styles from './ApiMetadataBlock.module.scss';

export type ApiMetadataField = 'name' | 'version' | 'description' | 'labels' | 'owner';

export const API_METADATA_FIELDS: readonly ApiMetadataField[] = [
    'name',
    'version',
    'description',
    'labels',
    'owner',
] as const;

export const API_METADATA_FIELD_LABELS: Record<ApiMetadataField, string> = {
    name: 'Name',
    version: 'Version',
    description: 'Description',
    labels: 'Labels',
    owner: 'Owner',
};

function MetadataChip({ field }: { readonly field: ApiMetadataField }) {
    return (
        <span className={styles.chip}>
            <svg className={styles.chipIcon} width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
                <path d="M4 7h16M4 12h10M4 17h6" />
            </svg>
            {API_METADATA_FIELD_LABELS[field]}
        </span>
    );
}

function LabelsView({ labels }: { readonly labels: string[] }) {
    const displayLabels = labels.filter(label => label !== 'MCP');
    const hasMcp = labels.includes('MCP');

    if (displayLabels.length === 0 && !hasMcp) {
        return <span className={styles.empty}>No labels</span>;
    }

    return (
        <div className={styles.labels}>
            {hasMcp && <span className={styles.mcpBadge}>MCP</span>}
            {displayLabels.map(label => (
                <span key={label} className={styles.label}>
                    {label}
                </span>
            ))}
        </div>
    );
}

function MetadataView({ field }: { readonly field: ApiMetadataField }) {
    const api = useApiData();

    if (!api) {
        return <span className={styles.empty}>—</span>;
    }

    switch (field) {
        case 'name':
            return <p className={styles.name}>{api.name}</p>;
        case 'version':
            return <p className={styles.version}>{api.version}</p>;
        case 'description':
            return (
                <p className={styles.description}>
                    {api.description || 'Description for this API is missing.'}
                </p>
            );
        case 'labels':
            return <LabelsView labels={api.labels ?? []} />;
        case 'owner':
            return <p className={styles.owner}>{api.owner?.displayName ?? '—'}</p>;
        default:
            return null;
    }
}

export const ApiMetadataBlock = createReactBlockSpec(
    {
        type: 'graviteeApiMetadata' as const,
        propSchema: {
            field: { default: 'name' as ApiMetadataField },
        },
        content: 'none',
    },
    {
        ...getGmdBlockHooks('graviteeApiMetadata'),
        render: ({ block, editor }) => {
            const field = block.props.field as ApiMetadataField;
            const isEditable = editor.isEditable;

            if (isEditable) {
                return <MetadataChip field={field} />;
            }

            return <MetadataView field={field} />;
        },
    },
);
