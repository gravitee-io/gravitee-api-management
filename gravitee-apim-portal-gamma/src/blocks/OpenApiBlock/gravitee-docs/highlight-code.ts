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
import hljs from 'highlight.js/lib/core';
import bash from 'highlight.js/lib/languages/bash';
import go from 'highlight.js/lib/languages/go';
import java from 'highlight.js/lib/languages/java';
import javascript from 'highlight.js/lib/languages/javascript';
import json from 'highlight.js/lib/languages/json';
import python from 'highlight.js/lib/languages/python';

import type { CodeSampleLanguage } from '../../ApiSpecBlock/code-sample-generator';

export type HighlightLanguage = 'bash' | 'javascript' | 'python' | 'java' | 'go' | 'json';

hljs.registerLanguage('bash', bash);
hljs.registerLanguage('javascript', javascript);
hljs.registerLanguage('python', python);
hljs.registerLanguage('java', java);
hljs.registerLanguage('go', go);
hljs.registerLanguage('json', json);

export const CODE_SAMPLE_HIGHLIGHT_LANGUAGE: Record<CodeSampleLanguage, HighlightLanguage> = {
    curl: 'bash',
    python: 'python',
    node: 'javascript',
    javascript: 'javascript',
    java: 'java',
    go: 'go',
};

export function highlightCode(code: string, language: HighlightLanguage): string {
    if (!code.trim()) {
        return '';
    }

    try {
        return hljs.highlight(code, { language }).value;
    } catch {
        return hljs.highlightAuto(code).value;
    }
}
