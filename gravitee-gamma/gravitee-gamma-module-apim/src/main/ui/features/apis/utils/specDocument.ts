/*
 * Copyright © 2015 The Gravitee team (http://gravitee.io)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
import { load, YAMLException } from 'js-yaml';

import type { PortalPageContentType } from '../types/apiDocumentation';

export type SpecContentType = Exclude<PortalPageContentType, 'GRAVITEE_MARKDOWN'>;

export type SpecDocument = Record<string, unknown>;

export type SpecDocumentResult = { status: 'valid'; document: SpecDocument } | { status: 'empty' } | { status: 'invalid'; message: string };

const SPEC_KINDS: Record<SpecContentType, { label: string; rootFields: string[]; rootFieldsLabel: string }> = {
    OPENAPI: { label: 'OpenAPI', rootFields: ['openapi', 'swagger'], rootFieldsLabel: 'an openapi or swagger' },
    ASYNCAPI: { label: 'AsyncAPI', rootFields: ['asyncapi'], rootFieldsLabel: 'an asyncapi' },
};

const BLANK_OR_COMMENT_LINE = /^\s*(#.*)?$/;

/**
 * Reads an OpenAPI or AsyncAPI document so a viewer only ever receives a parsed document of the right kind. JSON goes
 * through the YAML parser too, which accepts it and reports errors with a line number either way.
 */
export function parseSpecDocument(type: SpecContentType, content: string): SpecDocumentResult {
    if (content.split('\n').every(line => BLANK_OR_COMMENT_LINE.test(line))) {
        return { status: 'empty' };
    }

    let parsed: unknown;
    try {
        parsed = load(content);
    } catch (error: unknown) {
        if (error instanceof YAMLException) {
            const line = error.mark ? ` on line ${error.mark.line + 1}` : '';
            return { status: 'invalid', message: `YAML error${line}: ${error.reason}` };
        }
        throw error;
    }

    const kind = SPEC_KINDS[type];
    if (!isObject(parsed) || !kind.rootFields.some(field => field in parsed)) {
        return {
            status: 'invalid',
            message: `This doesn't look like an ${kind.label} document: it needs ${kind.rootFieldsLabel} field at the top level.`,
        };
    }
    return { status: 'valid', document: parsed };
}

function isObject(value: unknown): value is SpecDocument {
    return typeof value === 'object' && value !== null && !Array.isArray(value);
}
