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
import type { ParsedOpenApiSpec } from '../../ApiSpecBlock/openapi-spec-utils';
import styles from '../GraviteeDocsRenderer.module.scss';

import { GraviteeDocsOperationPanel } from './GraviteeDocsOperationPanel';
import { getOperationSectionId } from './gravitee-docs-utils';

interface GraviteeDocsOperationsContentProps {
    readonly spec: ParsedOpenApiSpec;
}

export function GraviteeDocsOperationsContent({ spec }: GraviteeDocsOperationsContentProps) {
    return (
        <main className={styles.centerPanel}>
            {spec.operations.map((operation, index) => (
                <GraviteeDocsOperationPanel
                    key={operation.operationId}
                    spec={spec}
                    operation={operation}
                    sectionId={getOperationSectionId(operation.operationId)}
                    isLast={index === spec.operations.length - 1}
                />
            ))}
        </main>
    );
}
