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
import { useMemo, useState } from 'react';

import {
    CODE_SAMPLE_LABELS,
    CODE_SAMPLE_LANGUAGES,
    generateCodeSample,
    type CodeSampleLanguage,
} from '../ApiSpecBlock/code-sample-generator';
import { useApiSpec } from '../ApiSpecBlock/ApiSpecContext';
import { BlockConfigChip, EmptyState, LoadingState, MethodBadge } from '../ApiSpecBlock/shared/ApiSpecShared';
import styles from '../ApiSpecBlock/shared/ApiSpecShared.module.scss';

interface ApiCodeSamplesViewProps {
    readonly tag: string;
    readonly operationId?: string;
    readonly serverUrlOverride?: string;
    readonly isEditable: boolean;
}

export function ApiCodeSamplesView({
    tag,
    operationId,
    serverUrlOverride,
    isEditable,
}: ApiCodeSamplesViewProps) {
    const { spec, getOperationsByTag, isLoading } = useApiSpec();
    const [activeLanguage, setActiveLanguage] = useState<CodeSampleLanguage>('curl');
    const operations = getOperationsByTag(tag, operationId || undefined);
    const [selectedOperationId, setSelectedOperationId] = useState(
        operationId ?? operations[0]?.operationId ?? '',
    );

    const activeOperation =
        operations.find(operation => operation.operationId === selectedOperationId) ?? operations[0];

    const sample = useMemo(() => {
        if (!spec?.document || !activeOperation) {
            return '';
        }
        return generateCodeSample(spec.document, activeOperation, activeLanguage, serverUrlOverride);
    }, [activeLanguage, activeOperation, serverUrlOverride, spec?.document]);

    if (isEditable) {
        return <BlockConfigChip label="API Code Samples" tag={tag || undefined} operationId={operationId || undefined} />;
    }

    if (isLoading) {
        return <LoadingState />;
    }

    if (operations.length === 0 || !spec?.document || !activeOperation) {
        return <EmptyState message={tag ? `No operations found for tag "${tag}".` : 'Configure a tag for this block.'} />;
    }

    return (
        <section className={styles.section}>
            {operations.length > 1 ? (
                <div className={styles.subsection}>
                    <label className={styles.fieldLabel} htmlFor="code-sample-operation">
                        Operation
                    </label>
                    <select
                        id="code-sample-operation"
                        className={styles.select}
                        value={activeOperation.operationId}
                        onChange={event => setSelectedOperationId(event.target.value)}
                    >
                        {operations.map(operation => (
                            <option key={operation.operationId} value={operation.operationId}>
                                {operation.method.toUpperCase()} {operation.path}
                            </option>
                        ))}
                    </select>
                </div>
            ) : (
                <div className={styles.sectionHeader}>
                    <MethodBadge method={activeOperation.method} />
                    <code className={styles.path}>{activeOperation.path}</code>
                </div>
            )}

            <div className={styles.tabs}>
                {CODE_SAMPLE_LANGUAGES.map(language => (
                    <button
                        key={language}
                        type="button"
                        className={language === activeLanguage ? styles.tabActive : styles.tab}
                        onClick={() => setActiveLanguage(language)}
                    >
                        {CODE_SAMPLE_LABELS[language]}
                    </button>
                ))}
            </div>

            <pre className={styles.codeBlock}>{sample}</pre>
        </section>
    );
}
