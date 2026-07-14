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
import { DownloadIcon } from '@gravitee/graphene-core/icons';
import { forwardRef } from 'react';

import type { ParsedOpenApiSpec } from '../../ApiSpecBlock/openapi-spec-utils';
import styles from '../GraviteeDocsRenderer.module.scss';

import { downloadSpecContent } from './gravitee-docs-utils';

interface GraviteeDocsHeaderProps {
    readonly spec: ParsedOpenApiSpec;
    readonly specContent: string;
}

export const GraviteeDocsHeader = forwardRef<HTMLElement, GraviteeDocsHeaderProps>(
    function GraviteeDocsHeader({ spec, specContent }, ref) {
        const title = spec.document.info?.title ?? 'API Documentation';
        const version = spec.document.info?.version;

        return (
            <header ref={ref} className={styles.headerMain}>
                <h1 className={styles.apiTitle}>{title}</h1>
                {version ? (
                    <span className={styles.versionBadge}>
                        {version}
                        <span className={styles.versionCurrent}>Current</span>
                    </span>
                ) : null}
                <Button
                    type="button"
                    variant="outline"
                    size="sm"
                    className={styles.downloadButton}
                    onClick={() => downloadSpecContent(specContent, title)}
                >
                    <DownloadIcon />
                    Download spec
                </Button>
            </header>
        );
    },
);
