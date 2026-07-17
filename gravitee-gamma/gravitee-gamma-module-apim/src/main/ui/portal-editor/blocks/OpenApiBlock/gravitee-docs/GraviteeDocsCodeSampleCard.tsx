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
import { SelectorDropdown, SelectorTriggerButton } from '@gravitee/graphene-core';
import { useMemo, useState } from 'react';

import {
    CODE_SAMPLE_LABELS,
    CODE_SAMPLE_LANGUAGES,
    generateCodeSample,
    type CodeSampleLanguage,
} from '../../ApiSpecBlock/code-sample-generator';
import type { ParsedOpenApiSpec, ParsedOperation } from '../../ApiSpecBlock/openapi-spec-utils';
import styles from '../GraviteeDocsRenderer.module.scss';

import { CodeLanguageIcon } from './CodeLanguageIcon';
import { copyToClipboard } from './gravitee-docs-utils';
import { CODE_SAMPLE_HIGHLIGHT_LANGUAGE } from './highlight-code';
import { HighlightedCodeBlock } from './HighlightedCodeBlock';

const CODE_SAMPLE_OPTIONS = CODE_SAMPLE_LANGUAGES.map(language => ({
    key: language,
    label: CODE_SAMPLE_LABELS[language],
    icon: <CodeLanguageIcon language={language} />,
}));

interface GraviteeDocsCodeSampleCardProps {
    readonly spec: ParsedOpenApiSpec;
    readonly operation: ParsedOperation;
}

export function GraviteeDocsCodeSampleCard({ spec, operation }: GraviteeDocsCodeSampleCardProps) {
    const [activeLanguage, setActiveLanguage] = useState<CodeSampleLanguage>('curl');
    const [copied, setCopied] = useState(false);

    const codeSample = useMemo(
        () => generateCodeSample(spec.document, operation, activeLanguage),
        [activeLanguage, operation, spec.document],
    );

    const handleCopy = async () => {
        await copyToClipboard(codeSample);
        setCopied(true);
        setTimeout(() => setCopied(false), 2000);
    };

    return (
        <div className={styles.rightCard}>
            <div className={styles.codeSampleHeader}>
                <SelectorDropdown
                    options={CODE_SAMPLE_OPTIONS}
                    activeKey={activeLanguage}
                    onChange={key => setActiveLanguage(key as CodeSampleLanguage)}
                    align="start"
                    renderTrigger={({ activeOption }) => (
                        <SelectorTriggerButton
                            activeOption={activeOption}
                            className={styles.codeSampleSelect}
                            aria-label="Code sample language"
                        />
                    )}
                />
                <button type="button" className={styles.copyButton} onClick={() => void handleCopy()}>
                    {copied ? 'Copied' : 'Copy'}
                </button>
            </div>
            <HighlightedCodeBlock
                code={codeSample}
                language={CODE_SAMPLE_HIGHLIGHT_LANGUAGE[activeLanguage]}
            />
        </div>
    );
}
