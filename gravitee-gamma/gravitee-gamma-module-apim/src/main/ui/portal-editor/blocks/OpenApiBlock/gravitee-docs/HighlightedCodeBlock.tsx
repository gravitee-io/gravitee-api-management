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
import { useMemo } from 'react';

import { highlightCode, type HighlightLanguage } from './highlight-code';
import styles from './HighlightedCodeBlock.module.scss';

interface HighlightedCodeBlockProps {
    readonly code: string;
    readonly language: HighlightLanguage;
    readonly className?: string;
}

export function HighlightedCodeBlock({ code, language, className }: HighlightedCodeBlockProps) {
    const highlighted = useMemo(() => highlightCode(code, language), [code, language]);

    return (
        <pre className={className ? `${styles.block} ${className}` : styles.block}>
            <code
                className={`hljs ${styles.code}`}
                dangerouslySetInnerHTML={{ __html: highlighted || code }}
            />
        </pre>
    );
}
