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
import { useEffect, useRef, useState } from 'react';

import { MethodBadge } from '../../ApiSpecBlock/shared/ApiSpecShared';
import type { ParsedOpenApiSpec, ParsedOperation } from '../../ApiSpecBlock/openapi-spec-utils';
import styles from '../GraviteeDocsRenderer.module.scss';

import { groupOperationsByTag } from './gravitee-docs-utils';

interface GraviteeDocsSidebarProps {
    readonly spec: ParsedOpenApiSpec;
    readonly activeOperationId: string;
    readonly onNavigateToOperation: (operationId: string) => void;
}

function OperationNavItem({
    operation,
    isActive,
    onSelect,
}: {
    readonly operation: ParsedOperation;
    readonly isActive: boolean;
    readonly onSelect: () => void;
}) {
    const itemRef = useRef<HTMLButtonElement>(null);
    const label = operation.summary ?? operation.operationId;

    useEffect(() => {
        if (isActive) {
            itemRef.current?.scrollIntoView?.({ block: 'nearest' });
        }
    }, [isActive]);

    return (
        <button
            ref={itemRef}
            type="button"
            className={isActive ? styles.navItemActive : styles.navItem}
            onClick={onSelect}
            aria-current={isActive ? 'true' : undefined}
        >
            <MethodBadge method={operation.method} />
            <span className={styles.navItemLabel}>{label}</span>
        </button>
    );
}

export function GraviteeDocsSidebar({
    spec,
    activeOperationId,
    onNavigateToOperation,
}: GraviteeDocsSidebarProps) {
    const [schemasExpanded, setSchemasExpanded] = useState(true);
    const grouped = groupOperationsByTag(spec.operations, spec.tags);
    const schemaNames = Object.keys(spec.document.components?.schemas ?? {});

    return (
        <aside className={styles.sidebar}>
            <section className={styles.sidebarSection}>
                <h2 className={styles.sidebarTitle}>Endpoints</h2>
                {grouped.map(({ tag, operations }) =>
                    operations.length > 0 ? (
                        <div key={tag} className={styles.tagGroup}>
                            <h3 className={styles.tagName}>{tag}</h3>
                            <nav className={styles.navList}>
                                {operations.map(operation => (
                                    <OperationNavItem
                                        key={operation.operationId}
                                        operation={operation}
                                        isActive={operation.operationId === activeOperationId}
                                        onSelect={() => onNavigateToOperation(operation.operationId)}
                                    />
                                ))}
                            </nav>
                        </div>
                    ) : null,
                )}
            </section>

            {schemaNames.length > 0 ? (
                <section className={styles.sidebarSection}>
                    <button
                        type="button"
                        className={styles.sidebarCollapsible}
                        onClick={() => setSchemasExpanded(current => !current)}
                    >
                        <span>Schemas</span>
                        <span className={styles.chevron}>{schemasExpanded ? '▾' : '▸'}</span>
                    </button>
                    {schemasExpanded ? (
                        <ul className={styles.schemaList}>
                            {schemaNames.map(name => (
                                <li key={name} className={styles.schemaListItem}>
                                    {name}
                                </li>
                            ))}
                        </ul>
                    ) : null}
                </section>
            ) : null}
        </aside>
    );
}
