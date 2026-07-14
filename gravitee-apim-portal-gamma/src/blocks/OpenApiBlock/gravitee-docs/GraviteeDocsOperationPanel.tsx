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
import { useState } from 'react';

import { buildRequestUrl } from '../../ApiSpecBlock/api-try-it-utils';
import { getDefaultServerUrl } from '../../ApiSpecBlock/openapi-spec-utils';
import type { ParsedOpenApiSpec, ParsedOperation } from '../../ApiSpecBlock/openapi-spec-utils';
import { MethodBadge } from '../../ApiSpecBlock/shared/ApiSpecShared';
import { SchemaTree } from '../../ApiSpecBlock/shared/SchemaTree';
import styles from '../GraviteeDocsRenderer.module.scss';

import {
    copyToClipboard,
    getParameterDefault,
    getParameterEnum,
    getParameterType,
    getPrimaryResponseMedia,
    getResponseSchema,
} from './gravitee-docs-utils';

interface GraviteeDocsOperationPanelProps {
    readonly spec: ParsedOpenApiSpec;
    readonly operation: ParsedOperation;
    readonly sectionId: string;
    readonly isLast?: boolean;
}

interface ParameterSectionProps {
    readonly title: string;
    readonly parameters: readonly OpenAPIV3.ParameterObject[];
}

function ParameterSection({ title, parameters }: ParameterSectionProps) {
    const [expanded, setExpanded] = useState(true);

    if (parameters.length === 0) {
        return null;
    }

    return (
        <section className={styles.paramSection}>
            <button
                type="button"
                className={styles.paramSectionHeader}
                onClick={() => setExpanded(current => !current)}
            >
                <span>{title}</span>
                <span className={styles.chevron}>{expanded ? '▾' : '▸'}</span>
            </button>
            {expanded ? (
                <div className={styles.paramList}>
                    {parameters.map(parameter => {
                        const paramType = getParameterType(parameter);
                        const defaultValue = getParameterDefault(parameter);
                        const enumValues = getParameterEnum(parameter);

                        return (
                            <div key={`${parameter.in}-${parameter.name}`} className={styles.paramItem}>
                                <div className={styles.paramHeader}>
                                    <span className={styles.paramName}>{parameter.name}</span>
                                    <span className={styles.paramType}>{paramType}</span>
                                    {parameter.required ? (
                                        <span className={styles.paramRequired}>required</span>
                                    ) : null}
                                </div>
                                {defaultValue ? (
                                    <p className={styles.paramMeta}>Default: {defaultValue}</p>
                                ) : null}
                                {enumValues ? (
                                    <p className={styles.paramMeta}>
                                        Allowed values: {enumValues.join(', ')}
                                    </p>
                                ) : null}
                                {parameter.description ? (
                                    <p className={styles.paramDescription}>{parameter.description}</p>
                                ) : null}
                            </div>
                        );
                    })}
                </div>
            ) : null}
        </section>
    );
}

export function GraviteeDocsOperationPanel({
    spec,
    operation,
    sectionId,
    isLast = false,
}: GraviteeDocsOperationPanelProps) {
    const [copied, setCopied] = useState(false);
    const serverUrl = getDefaultServerUrl(spec.document);
    const fullUrl = buildRequestUrl(operation, serverUrl, {}, {});
    const title = operation.summary ?? operation.operationId;

    const pathParams = operation.parameters.filter(parameter => parameter.in === 'path');
    const queryParams = operation.parameters.filter(parameter => parameter.in === 'query');
    const headerParams = operation.parameters.filter(parameter => parameter.in === 'header');

    const responseMedia = getPrimaryResponseMedia(operation);
    const responseSchema =
        responseMedia && spec.document
            ? getResponseSchema(spec.document, responseMedia.media)
            : undefined;

    const handleCopy = async () => {
        await copyToClipboard(fullUrl);
        setCopied(true);
        setTimeout(() => setCopied(false), 2000);
    };

    return (
        <section
            id={sectionId}
            data-operation-id={operation.operationId}
            className={isLast ? styles.operationSectionLast : styles.operationSection}
        >
            <h2 className={styles.operationTitle}>{title}</h2>

            <div className={styles.urlBar}>
                <MethodBadge method={operation.method} />
                <code className={styles.urlText}>{fullUrl}</code>
                <button type="button" className={styles.copyButton} onClick={() => void handleCopy()}>
                    {copied ? 'Copied' : 'Copy'}
                </button>
            </div>

            {operation.description ? (
                <p className={styles.operationDescription}>{operation.description}</p>
            ) : null}

            <ParameterSection title="Path Parameters" parameters={pathParams} />
            <ParameterSection title="Query Parameters" parameters={queryParams} />
            <ParameterSection title="Header Parameters" parameters={headerParams} />

            {operation.requestBody ? (
                <section className={styles.paramSection}>
                    <div className={styles.paramSectionHeaderStatic}>Request Body</div>
                    <p className={styles.paramDescription}>
                        {operation.requestBody.description ?? 'Request payload'}
                    </p>
                </section>
            ) : null}

            {responseMedia ? (
                <section className={styles.responseSection}>
                    <h3 className={styles.responseTitle}>Response</h3>
                    <div className={styles.responseStatus}>
                        <span className={styles.statusDot} />
                        <span>
                            {responseMedia.status} {responseMedia.contentType}
                        </span>
                    </div>
                    {responseMedia.description ? (
                        <p className={styles.responseDescription}>{responseMedia.description}</p>
                    ) : null}
                    {responseSchema ? (
                        <div className={styles.responseSchema}>
                            <SchemaTree name="response" schema={responseSchema} />
                        </div>
                    ) : null}
                </section>
            ) : null}
        </section>
    );
}
