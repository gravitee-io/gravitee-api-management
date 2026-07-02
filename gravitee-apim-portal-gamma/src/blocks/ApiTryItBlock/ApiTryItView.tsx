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

import { useApiSpec } from '../ApiSpecBlock/ApiSpecContext';
import { getDefaultServerUrl } from '../ApiSpecBlock/openapi-spec-utils';
import type { ParsedOperation } from '../ApiSpecBlock/openapi-spec-utils';
import { BlockConfigChip, EmptyState, LoadingState, MethodBadge } from '../ApiSpecBlock/shared/ApiSpecShared';
import styles from '../ApiSpecBlock/shared/ApiSpecShared.module.scss';

interface ApiTryItViewProps {
    readonly tag: string;
    readonly operationId?: string;
    readonly serverUrlOverride?: string;
    readonly authType: string;
    readonly authValue: string;
    readonly isEditable: boolean;
}

interface TryItResponse {
    readonly status: number;
    readonly statusText: string;
    readonly durationMs: number;
    readonly body: string;
    readonly ok: boolean;
}

function buildRequestUrl(
    operation: ParsedOperation,
    serverUrl: string,
    pathValues: Record<string, string>,
    queryValues: Record<string, string>,
): string {
    let path = operation.path;
    for (const [name, value] of Object.entries(pathValues)) {
        path = path.replace(`{${name}}`, encodeURIComponent(value));
    }

    const query = new URLSearchParams();
    for (const [name, value] of Object.entries(queryValues)) {
        if (value) {
            query.set(name, value);
        }
    }

    const normalizedServer = serverUrl.replace(/\/$/, '');
    const queryString = query.toString();
    return `${normalizedServer}${path}${queryString ? `?${queryString}` : ''}`;
}

function getJsonContentType(operation: ParsedOperation): string | undefined {
    const content = operation.requestBody?.content;
    if (!content) {
        return undefined;
    }
    if (content['application/json']) {
        return 'application/json';
    }
    return Object.keys(content)[0];
}

async function executeTryItRequest(
    operation: ParsedOperation,
    serverUrl: string,
    pathValues: Record<string, string>,
    queryValues: Record<string, string>,
    headerValues: Record<string, string>,
    body: string,
    authType: string,
    authValue: string,
): Promise<TryItResponse> {
    const url = buildRequestUrl(operation, serverUrl, pathValues, queryValues);
    const headers = new Headers(headerValues);

    const contentType = getJsonContentType(operation);
    if (contentType && body.trim()) {
        headers.set('Content-Type', contentType);
    }

    if (authType === 'bearer' && authValue.trim()) {
        headers.set('Authorization', `Bearer ${authValue.trim()}`);
    }
    if (authType === 'apiKey' && authValue.trim()) {
        headers.set('X-API-Key', authValue.trim());
    }

    const startedAt = performance.now();
    const response = await fetch(url, {
        method: operation.method.toUpperCase(),
        headers,
        body: ['GET', 'HEAD'].includes(operation.method.toUpperCase()) ? undefined : body || undefined,
    });
    const durationMs = Math.round(performance.now() - startedAt);
    const text = await response.text();

    return {
        status: response.status,
        statusText: response.statusText,
        durationMs,
        body: text,
        ok: response.ok,
    };
}

export function ApiTryItView({
    tag,
    operationId,
    serverUrlOverride,
    authType,
    authValue,
    isEditable,
}: ApiTryItViewProps) {
    const { spec, getOperationsByTag, isLoading } = useApiSpec();
    const operations = getOperationsByTag(tag, operationId || undefined);
    const [selectedOperationId, setSelectedOperationId] = useState(operationId ?? operations[0]?.operationId ?? '');
    const [pathValues, setPathValues] = useState<Record<string, string>>({});
    const [queryValues, setQueryValues] = useState<Record<string, string>>({});
    const [headerValues, setHeaderValues] = useState<Record<string, string>>({});
    const [body, setBody] = useState('{\n  \n}');
    const [response, setResponse] = useState<TryItResponse | null>(null);
    const [error, setError] = useState<string | null>(null);
    const [isSending, setIsSending] = useState(false);

    const activeOperation = useMemo(
        () => operations.find(operation => operation.operationId === selectedOperationId) ?? operations[0],
        [operations, selectedOperationId],
    );

    if (isEditable) {
        return <BlockConfigChip label="API Try It" tag={tag || undefined} operationId={operationId || undefined} />;
    }

    if (isLoading) {
        return <LoadingState />;
    }

    if (!activeOperation || !spec?.document) {
        return <EmptyState message={tag ? `No operations found for tag "${tag}".` : 'Configure a tag for this block.'} />;
    }

    const serverUrl = getDefaultServerUrl(spec.document, serverUrlOverride);
    const pathParameters = activeOperation.parameters.filter(parameter => parameter.in === 'path');
    const queryParameters = activeOperation.parameters.filter(parameter => parameter.in === 'query');
    const headerParameters = activeOperation.parameters.filter(parameter => parameter.in === 'header');
    const hasRequestBody = Boolean(activeOperation.requestBody);

    const handleSend = async () => {
        setIsSending(true);
        setError(null);
        try {
            const result = await executeTryItRequest(
                activeOperation,
                serverUrl,
                pathValues,
                queryValues,
                headerValues,
                body,
                authType,
                authValue,
            );
            setResponse(result);
        } catch (requestError) {
            setResponse(null);
            setError(requestError instanceof Error ? requestError.message : 'Request failed');
        } finally {
            setIsSending(false);
        }
    };

    return (
        <section className={styles.section}>
            <div className={styles.sectionHeader}>
                <MethodBadge method={activeOperation.method} />
                <code className={styles.path}>{activeOperation.path}</code>
            </div>

            <div className={styles.subsection}>
                <div className={styles.formGrid}>
                    {operations.length > 1 ? (
                        <label className={styles.field}>
                            <span className={styles.fieldLabel}>Operation</span>
                            <select
                                className={styles.select}
                                value={activeOperation.operationId}
                                onChange={event => setSelectedOperationId(event.target.value)}
                            >
                                {operations.map(operation => (
                                    <option key={operation.operationId} value={operation.operationId}>
                                        {operation.method.toUpperCase()} {operation.path}
                                    </option>
                                ))}
                            </select>
                        </label>
                    ) : null}

                    <label className={styles.field}>
                        <span className={styles.fieldLabel}>Server URL</span>
                        <input className={styles.input} value={serverUrl} readOnly />
                    </label>

                    {pathParameters.map(parameter => (
                        <label key={parameter.name} className={styles.field}>
                            <span className={styles.fieldLabel}>
                                {parameter.name} (path){parameter.required ? ' *' : ''}
                            </span>
                            <input
                                className={styles.input}
                                value={pathValues[parameter.name] ?? ''}
                                onChange={event =>
                                    setPathValues(current => ({
                                        ...current,
                                        [parameter.name]: event.target.value,
                                    }))
                                }
                            />
                        </label>
                    ))}

                    {queryParameters.map(parameter => (
                        <label key={parameter.name} className={styles.field}>
                            <span className={styles.fieldLabel}>
                                {parameter.name} (query){parameter.required ? ' *' : ''}
                            </span>
                            <input
                                className={styles.input}
                                value={queryValues[parameter.name] ?? ''}
                                onChange={event =>
                                    setQueryValues(current => ({
                                        ...current,
                                        [parameter.name]: event.target.value,
                                    }))
                                }
                            />
                        </label>
                    ))}

                    {headerParameters.map(parameter => (
                        <label key={parameter.name} className={styles.field}>
                            <span className={styles.fieldLabel}>
                                {parameter.name} (header){parameter.required ? ' *' : ''}
                            </span>
                            <input
                                className={styles.input}
                                value={headerValues[parameter.name] ?? ''}
                                onChange={event =>
                                    setHeaderValues(current => ({
                                        ...current,
                                        [parameter.name]: event.target.value,
                                    }))
                                }
                            />
                        </label>
                    ))}

                    {hasRequestBody ? (
                        <label className={styles.field}>
                            <span className={styles.fieldLabel}>Request body</span>
                            <textarea
                                className={styles.textarea}
                                value={body}
                                onChange={event => setBody(event.target.value)}
                            />
                        </label>
                    ) : null}

                    <button type="button" className={styles.button} disabled={isSending} onClick={() => void handleSend()}>
                        {isSending ? 'Sending…' : 'Send request'}
                    </button>
                </div>
            </div>

            {error ? <p className={styles.description}>{error}</p> : null}

            {response ? (
                <div>
                    <div className={styles.responseMeta}>
                        <span className={response.ok ? styles.statusSuccess : styles.statusError}>
                            {response.status} {response.statusText}
                        </span>
                        <span>{response.durationMs} ms</span>
                    </div>
                    <pre className={styles.codeBlock}>{response.body || '(empty response)'}</pre>
                </div>
            ) : null}
        </section>
    );
}
