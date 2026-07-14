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

import type { ParsedOpenApiSpec } from '../../ApiSpecBlock/openapi-spec-utils';
import styles from '../GraviteeDocsRenderer.module.scss';

import { GraviteeDocsHeader } from './GraviteeDocsHeader';
import { GraviteeDocsOperationPanel } from './GraviteeDocsOperationPanel';
import { GraviteeDocsRightPanel } from './GraviteeDocsRightPanel';
import { GraviteeDocsSidebar } from './GraviteeDocsSidebar';

interface GraviteeDocsShellProps {
    readonly spec: ParsedOpenApiSpec;
    readonly specContent: string;
}

export function GraviteeDocsShell({ spec, specContent }: GraviteeDocsShellProps) {
    const [selectedOperationId, setSelectedOperationId] = useState(
        spec.operations[0]?.operationId ?? '',
    );

    const activeOperation = useMemo(
        () =>
            spec.operations.find(operation => operation.operationId === selectedOperationId) ??
            spec.operations[0],
        [selectedOperationId, spec.operations],
    );

    if (!activeOperation) {
        return <p className={styles.empty}>No operations found in this OpenAPI specification.</p>;
    }

    return (
        <div className={styles.shell} data-testid="gravitee-docs-renderer">
            <GraviteeDocsHeader spec={spec} specContent={specContent} />
            <div className={styles.layout}>
                <GraviteeDocsSidebar
                    spec={spec}
                    selectedOperationId={activeOperation.operationId}
                    onSelectOperation={setSelectedOperationId}
                />
                <GraviteeDocsOperationPanel spec={spec} operation={activeOperation} />
                <GraviteeDocsRightPanel spec={spec} operation={activeOperation} />
            </div>
        </div>
    );
}
