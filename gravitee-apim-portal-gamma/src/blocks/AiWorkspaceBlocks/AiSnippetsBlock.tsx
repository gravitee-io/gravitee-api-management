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
import { CopyButton, isTruthyProp } from './shared';
import {
    buildSnippet,
    parseLanguages,
    SNIPPET_LANGUAGE_LABELS,
    type SnippetLanguage,
} from './snippets';
import { HighlightedCodeBlock } from '../OpenApiBlock/gravitee-docs/HighlightedCodeBlock';
import type { HighlightLanguage } from '../OpenApiBlock/gravitee-docs/highlight-code';

const SNIPPET_HIGHLIGHT_LANGUAGE: Record<SnippetLanguage, HighlightLanguage> = {
    curl: 'bash',
    python: 'python',
    javascript: 'javascript',
    java: 'java',
    go: 'go',
};

export function AiSnippetsView({
    workspaceId,
    languagesRaw,
    useRealKey,
}: {
    workspaceId: string;
    languagesRaw: string;
    useRealKey: boolean;
}) {
    const workspace = getAiWorkspaceData(workspaceId);
    const languages = parseLanguages(languagesRaw);
    const [active, setActive] = useState<SnippetLanguage>(languages[0]);
    const activeLanguage = languages.includes(active) ? active : languages[0];

    const key = useRealKey ? workspace.aiKey : 'YOUR_AI_KEY';
    const model = workspace.models[0]?.id ?? 'gpt-4o';
    const snippet = buildSnippet(activeLanguage, {
        endpoint: workspace.endpoint,
        key,
        model,
    });

    return (
        <div className={styles.block}>
            <div className={styles.tabs}>
                {languages.map(language => (
                    <button
                        key={language}
                        type="button"
                        className={language === activeLanguage ? styles.tabActive : styles.tab}
                        onClick={() => setActive(language)}
                    >
                        {SNIPPET_LANGUAGE_LABELS[language]}
                    </button>
                ))}
            </div>
            <div className={styles.snippetWrap}>
                <CopyButton value={snippet} className={styles.snippetCopy} />
                <HighlightedCodeBlock
                    code={snippet}
                    language={SNIPPET_HIGHLIGHT_LANGUAGE[activeLanguage]}
                    className={styles.snippet}
                />
            </div>
        </div>
    );
}

export const AiSnippetsBlock = createReactBlockSpec(
    {
        type: 'graviteeAiSnippets' as const,
        propSchema: {
            workspaceId: { default: '' },
            languages: { default: '' },
            useRealKey: { default: 'false' },
        },
        content: 'none',
    },
    {
        ...getGmdBlockHooks('graviteeAiSnippets'),
        render: ({ block, editor }) => {
            const { workspaceId, languages, useRealKey } = block.props;
            const view = (
                <AiSnippetsView
                    workspaceId={workspaceId}
                    languagesRaw={languages}
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
                    <div className={styles.editHeader}>AI Code Snippets</div>
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
