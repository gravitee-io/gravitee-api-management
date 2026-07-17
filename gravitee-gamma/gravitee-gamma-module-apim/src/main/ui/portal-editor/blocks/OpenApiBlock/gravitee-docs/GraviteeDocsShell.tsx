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
import { useLayoutEffect, useMemo, useRef } from 'react';

import type { ParsedOpenApiSpec } from '../../ApiSpecBlock/openapi-spec-utils';
import styles from '../GraviteeDocsRenderer.module.scss';

import { GraviteeDocsApiDescription } from './GraviteeDocsApiDescription';
import { GraviteeDocsHeader } from './GraviteeDocsHeader';
import { GraviteeDocsOperationsContent } from './GraviteeDocsOperationsContent';
import { GraviteeDocsRightPanel } from './GraviteeDocsRightPanel';
import { GraviteeDocsSidebar } from './GraviteeDocsSidebar';
import { useOperationScrollSpy } from './useOperationScrollSpy';

interface GraviteeDocsShellProps {
    readonly spec: ParsedOpenApiSpec;
    readonly specContent: string;
}

export function GraviteeDocsShell({ spec, specContent }: GraviteeDocsShellProps) {
    const shellRef = useRef<HTMLDivElement>(null);
    const headerRef = useRef<HTMLElement>(null);

    useLayoutEffect(() => {
        const shell = shellRef.current;
        const header = headerRef.current;
        if (!shell || !header) {
            return undefined;
        }

        const updateOffset = () => {
            shell.style.setProperty('--docs-sticky-header-offset', `${header.offsetHeight}px`);
        };

        updateOffset();
        const observer = new ResizeObserver(updateOffset);
        observer.observe(header);

        return () => observer.disconnect();
    }, []);

    const operationIds = useMemo(
        () => spec.operations.map(operation => operation.operationId),
        [spec.operations],
    );

    const { activeOperationId, scrollToOperation } = useOperationScrollSpy({
        operationIds,
        defaultOperationId: operationIds[0],
        docsRootRef: shellRef,
    });

    const activeOperation = useMemo(
        () =>
            spec.operations.find(operation => operation.operationId === activeOperationId) ??
            spec.operations[0],
        [activeOperationId, spec.operations],
    );

    if (!activeOperation || spec.operations.length === 0) {
        return <p className={styles.empty}>No operations found in this OpenAPI specification.</p>;
    }

    return (
        <div ref={shellRef} className={styles.shell} data-testid="gravitee-docs-renderer">
            <GraviteeDocsHeader ref={headerRef} spec={spec} specContent={specContent} />
            <GraviteeDocsApiDescription spec={spec} />
            <div className={styles.layout}>
                <GraviteeDocsSidebar
                    spec={spec}
                    activeOperationId={activeOperation.operationId}
                    onNavigateToOperation={scrollToOperation}
                />
                <div className={styles.contentArea}>
                    <GraviteeDocsOperationsContent spec={spec} />
                    <GraviteeDocsRightPanel
                        key={activeOperation.operationId}
                        spec={spec}
                        operation={activeOperation}
                    />
                </div>
            </div>
        </div>
    );
}
