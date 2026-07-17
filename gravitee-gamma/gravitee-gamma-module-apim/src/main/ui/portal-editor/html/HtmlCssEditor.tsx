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
import { CodeEditor } from '@gravitee/graphene-core/code-editor';

import styles from './HtmlEditorShell.module.scss';

interface HtmlCssEditorProps {
    readonly value: string;
    readonly onChange: (value: string) => void;
    readonly className?: string;
    readonly height?: number | string;
    readonly fill?: boolean;
}

export function HtmlCssEditor({
    value,
    onChange,
    className,
    height = 300,
    fill = false,
}: HtmlCssEditorProps) {
    return (
        <CodeEditor
            language="css"
            value={value}
            onChange={next => onChange(next ?? '')}
            aria-label="CSS"
            className={className ?? (fill ? styles.monacoEditorFill : styles.monacoEditor)}
            height={fill ? '100%' : height}
        />
    );
}
