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
import { loader } from '@monaco-editor/react';
import * as monaco from 'monaco-editor/esm/vs/editor/editor.api';

// Self-host Monaco from the bundled package instead of @monaco-editor/react's default jsDelivr CDN,
// which is unreachable in firewalled/air-gapped deployments and flaky under CDN outages.
self.MonacoEnvironment = {
    getWorker: () => new Worker(new URL('monaco-editor/esm/vs/editor/editor.worker.js', import.meta.url)),
};

loader.config({ monaco });
