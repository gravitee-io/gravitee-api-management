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

import { setupCodeEditor } from '@gravitee/graphene-core';

// Wires up language workers so JSON validation, autocompletion, and diagnostics work.
// MonacoEnvironment has no dependency on monaco-editor itself, so it is set synchronously.
// Rspack resolves `new URL(path, import.meta.url)` at build time, emitting each worker
// as a separate hashed chunk served from the app's own origin — CSP-safe, offline-capable.
window.MonacoEnvironment = {
    getWorker(_moduleId: string, label: string): Worker {
        if (label === 'json') {
            return new Worker(new URL('monaco-editor/esm/vs/language/json/json.worker', import.meta.url));
        }
        if (label === 'css' || label === 'scss' || label === 'less') {
            return new Worker(new URL('monaco-editor/esm/vs/language/css/css.worker', import.meta.url));
        }
        if (label === 'html' || label === 'handlebars' || label === 'razor') {
            return new Worker(new URL('monaco-editor/esm/vs/language/html/html.worker', import.meta.url));
        }
        if (label === 'typescript' || label === 'javascript') {
            return new Worker(new URL('monaco-editor/esm/vs/language/typescript/ts.worker', import.meta.url));
        }
        return new Worker(new URL('monaco-editor/esm/vs/editor/editor.worker', import.meta.url));
    },
};

// Monaco is loaded lazily into a separate async chunk — keeps the initial bundle lightweight.
// Resolves well before any <CodeEditor> mounts since no editor appears on the initial route.
void import('monaco-editor').then(monaco => {
    setupCodeEditor({ monaco });
});
