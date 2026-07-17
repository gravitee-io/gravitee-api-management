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

import { parseOpenApiDocument } from '../ApiSpecBlock/openapi-spec-utils';

import { GraviteeDocsShell } from './gravitee-docs/GraviteeDocsShell';
import styles from './GraviteeDocsRenderer.module.scss';

interface GraviteeDocsRendererProps {
    readonly specContent: string;
}

export function GraviteeDocsRenderer({ specContent }: GraviteeDocsRendererProps) {
    const spec = useMemo(() => parseOpenApiDocument(specContent), [specContent]);

    if (!spec) {
        return <p className={styles.empty}>Unable to render spec with Gravitee Docs.</p>;
    }

    return <GraviteeDocsShell spec={spec} specContent={specContent} />;
}
