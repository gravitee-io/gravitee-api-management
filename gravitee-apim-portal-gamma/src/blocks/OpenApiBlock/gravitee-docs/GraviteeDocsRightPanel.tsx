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
import { Button } from '@gravitee/graphene-core';
import { useMemo, useState } from 'react';

import { executeTryItRequest, type TryItResponse } from '../../ApiSpecBlock/api-try-it-utils';
import { getDefaultServerUrl } from '../../ApiSpecBlock/openapi-spec-utils';
import type { ParsedOpenApiSpec, ParsedOperation } from '../../ApiSpecBlock/openapi-spec-utils';
import { MethodBadge } from '../../ApiSpecBlock/shared/ApiSpecShared';
import styles from '../GraviteeDocsRenderer.module.scss';

import { GraviteeDocsCodeSampleCard } from './GraviteeDocsCodeSampleCard';
import { getExampleResponse, getPrimaryResponseMedia } from './gravitee-docs-utils';
import { HighlightedCodeBlock } from './HighlightedCodeBlock';

interface GraviteeDocsRightPanelProps {
    readonly spec: ParsedOpenApiSpec;
    readonly operation: ParsedOperation;
}

export function GraviteeDocsRightPanel({ spec, operation }: GraviteeDocsRightPanelProps) {
    const serverUrl = getDefaultServerUrl(spec.document);
    const pathParameters = operation.parameters.filter(parameter => parameter.in === 'path');
    const queryParameters = operation.parameters.filter(parameter => parameter.in === 'query');
    const headerParameters = operation.parameters.filter(parameter => parameter.in === 'header');
    const hasRequestBody = Boolean(operation.requestBody);

    const [pathValues, setPathValues] = useState<Record<string, string>>({});
    const [queryValues, setQueryValues] = useState<Record<string, string>>({});
    const [headerValues, setHeaderValues] = useState<Record<string, string>>({});
    const [body, setBody] = useState('{\n  \n}');
    const [response, setResponse] = useState<TryItResponse | null>(null);
    const [error, setError] = useState<string | null>(null);
    const [isSending, setIsSending] = useState(false);

    const exampleResponse = useMemo(
        () => getExampleResponse(spec.document, operation),
        [operation, spec.document],
    );

    const responseMedia = getPrimaryResponseMedia(operation);

    const handleSend = async () => {
        setIsSending(true);
        setError(null);
        try {
            const result = await executeTryItRequest(
                operation,
                serverUrl,
                pathValues,
                queryValues,
                headerValues,
                body,
                '',
                '',
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
        <aside className={styles.rightPanel}>
            <div className={styles.rightCard}>
                <div className={styles.rightCardHeader}>
                    <MethodBadge method={operation.method} />
                    <span className={styles.rightCardPath}>{operation.path}</span>
                </div>
                <div className={styles.tryItForm}>
                    {pathParameters.map(parameter => (
                        <label key={parameter.name} className={styles.tryItField}>
                            <span className={styles.tryItLabel}>
                                {parameter.name}
                                {parameter.required ? ' *' : ''}
                            </span>
                            <input
                                className={styles.tryItInput}
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
                        <label key={parameter.name} className={styles.tryItField}>
                            <span className={styles.tryItLabel}>
                                {parameter.name}
                                {parameter.required ? ' *' : ''}
                            </span>
                            <input
                                className={styles.tryItInput}
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
                        <label key={parameter.name} className={styles.tryItField}>
                            <span className={styles.tryItLabel}>
                                {parameter.name}
                                {parameter.required ? ' *' : ''}
                            </span>
                            <input
                                className={styles.tryItInput}
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
                        <label className={styles.tryItField}>
                            <span className={styles.tryItLabel}>Body</span>
                            <textarea
                                className={styles.tryItTextarea}
                                value={body}
                                onChange={event => setBody(event.target.value)}
                            />
                        </label>
                    ) : null}

                    <Button
                        type="button"
                        className={styles.tryItButton}
                        disabled={isSending}
                        onClick={() => void handleSend()}
                    >
                        {isSending ? 'Sending…' : 'Try It!'}
                    </Button>
                </div>

                {error ? <p className={styles.tryItError}>{error}</p> : null}

                {response ? (
                    <div className={styles.tryItResponse}>
                        <div className={styles.responseMeta}>
                            <span className={response.ok ? styles.statusSuccess : styles.statusError}>
                                {response.status} {response.statusText}
                            </span>
                            <span>{response.durationMs} ms</span>
                        </div>
                        <HighlightedCodeBlock
                            code={response.body || '(empty response)'}
                            language="json"
                        />
                    </div>
                ) : null}
            </div>

            <GraviteeDocsCodeSampleCard spec={spec} operation={operation} />

            {exampleResponse ? (
                <div className={styles.rightCard}>
                    <div className={styles.rightCardTitle}>
                        {responseMedia
                            ? `${responseMedia.status} ${responseMedia.contentType}`
                            : 'Example response'}
                    </div>
                    <HighlightedCodeBlock code={exampleResponse} language="json" />
                </div>
            ) : null}
        </aside>
    );
}
