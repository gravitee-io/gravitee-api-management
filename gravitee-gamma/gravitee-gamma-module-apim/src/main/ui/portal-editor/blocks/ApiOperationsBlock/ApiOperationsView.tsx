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
import type { OpenAPIV3 } from 'openapi-types';

import { useApiSpec } from '../ApiSpecBlock/ApiSpecContext';
import type { ParsedOperation } from '../ApiSpecBlock/openapi-spec-utils';
import { BlockConfigChip, EmptyState, LoadingState, MethodBadge } from '../ApiSpecBlock/shared/ApiSpecShared';
import styles from '../ApiSpecBlock/shared/ApiSpecShared.module.scss';

interface ApiOperationsViewProps {
    readonly tag: string;
    readonly operationId?: string;
    readonly showResponses: boolean;
    readonly isEditable: boolean;
}

function ParameterTable({ parameters }: { readonly parameters: readonly OpenAPIV3.ParameterObject[] }) {
    if (parameters.length === 0) {
        return <EmptyState message="No parameters." />;
    }

    return (
        <table className={styles.table}>
            <thead>
                <tr>
                    <th>Name</th>
                    <th>In</th>
                    <th>Type</th>
                    <th>Description</th>
                </tr>
            </thead>
            <tbody>
                {parameters.map(parameter => (
                    <tr key={`${parameter.in}-${parameter.name}`}>
                        <td>
                            {parameter.name}
                            {parameter.required ? <span className={styles.required}> *</span> : null}
                        </td>
                        <td>{parameter.in}</td>
                        <td>
                            <span className={styles.typeBadge}>
                                {parameter.schema && 'type' in parameter.schema
                                    ? String(parameter.schema.type ?? 'string')
                                    : 'string'}
                            </span>
                        </td>
                        <td>{parameter.description ?? '—'}</td>
                    </tr>
                ))}
            </tbody>
        </table>
    );
}

function ResponseTable({ responses }: { readonly responses: OpenAPIV3.ResponsesObject }) {
    const entries = Object.entries(responses);
    if (entries.length === 0) {
        return <EmptyState message="No responses documented." />;
    }

    return (
        <table className={styles.table}>
            <thead>
                <tr>
                    <th>Status</th>
                    <th>Description</th>
                </tr>
            </thead>
            <tbody>
                {entries.map(([status, response]) => {
                    const resolved = '$ref' in response ? undefined : response;
                    return (
                        <tr key={status}>
                            <td>{status}</td>
                            <td>{resolved?.description ?? '—'}</td>
                        </tr>
                    );
                })}
            </tbody>
        </table>
    );
}

function OperationSection({
    operation,
    showResponses,
}: {
    readonly operation: ParsedOperation;
    readonly showResponses: boolean;
}) {
    return (
        <section className={styles.section}>
            <div className={styles.sectionHeader}>
                <MethodBadge method={operation.method} />
                <code className={styles.path}>{operation.path}</code>
            </div>
            {operation.summary ? <p className={styles.summary}>{operation.summary}</p> : null}
            {operation.description ? <p className={styles.description}>{operation.description}</p> : null}

            <div className={styles.subsection}>
                <h4 className={styles.subsectionTitle}>Parameters</h4>
                <ParameterTable parameters={operation.parameters} />
            </div>

            {operation.requestBody ? (
                <div className={styles.subsection}>
                    <h4 className={styles.subsectionTitle}>Request Body</h4>
                    <p className={styles.description}>{operation.requestBody.description ?? 'Request payload'}</p>
                </div>
            ) : null}

            {showResponses ? (
                <div className={styles.subsection}>
                    <h4 className={styles.subsectionTitle}>Responses</h4>
                    <ResponseTable responses={operation.responses} />
                </div>
            ) : null}
        </section>
    );
}

export function ApiOperationsView({ tag, operationId, showResponses, isEditable }: ApiOperationsViewProps) {
    const { getOperationsByTag, isLoading } = useApiSpec();

    if (isEditable) {
        return <BlockConfigChip label="API Operations" tag={tag || undefined} operationId={operationId || undefined} />;
    }

    if (isLoading) {
        return <LoadingState />;
    }

    const operations = getOperationsByTag(tag, operationId || undefined);
    if (operations.length === 0) {
        return <EmptyState message={tag ? `No operations found for tag "${tag}".` : 'Configure a tag for this block.'} />;
    }

    return (
        <div>
            {operations.map(operation => (
                <OperationSection
                    key={operation.operationId}
                    operation={operation}
                    showResponses={showResponses}
                />
            ))}
        </div>
    );
}
