/*
 * Copyright © 2015 The Gravitee team (http://gravitee.io)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
import Editor, { type EditorProps, type OnMount } from '@monaco-editor/react';
import type * as monacoNs from 'monaco-editor';
import { GAPL_LANGUAGE_ID, registerGaplLanguage } from './gapl-language';

export interface MonacoEditorProps {
    readonly value: string;
    readonly onChange?: (value: string) => void;
    readonly language?: string;
    readonly readOnly?: boolean;
    readonly height?: string | number;
    readonly theme?: 'light' | 'vs-dark' | 'system';
    readonly options?: EditorProps['options'];
    readonly ariaLabel?: string;
    readonly onMount?: (editor: monacoNs.editor.IStandaloneCodeEditor, monaco: typeof monacoNs) => void;
}

export function MonacoEditor({
    value,
    onChange,
    language = GAPL_LANGUAGE_ID,
    readOnly = false,
    height = '360px',
    theme = 'light',
    options,
    ariaLabel,
    onMount,
}: MonacoEditorProps) {
    const handleMount: OnMount = (editor, monaco) => {
        registerGaplLanguage(monaco);
        onMount?.(editor, monaco);
    };

    const monacoTheme = theme === 'vs-dark' ? 'vs-dark' : 'vs';

    return (
        <div aria-label={ariaLabel} role={ariaLabel ? 'group' : undefined}>
            <Editor
                height={height}
                language={language}
                theme={monacoTheme}
                value={value}
                onChange={v => onChange?.(v ?? '')}
                onMount={handleMount}
                options={{
                    // Monaco quirk: editor collapses to ~20px after container reflow without this.
                    automaticLayout: true,
                    readOnly,
                    minimap: { enabled: false },
                    scrollBeyondLastLine: false,
                    fontSize: 13,
                    lineNumbers: 'on',
                    renderLineHighlight: 'all',
                    tabSize: 2,
                    wordWrap: 'on',
                    ...options,
                }}
            />
        </div>
    );
}
