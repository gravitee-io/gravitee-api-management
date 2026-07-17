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
import type { BlockNoteDocument } from '../../portals/types';
import { getOpenApiSpec } from '../../editor/services/openapi.service';
import { looksLikeMarkdown, markdownToBlocks } from '../../editor/utils/markdown-to-blocks';

import { extractTags, parseOpenApiDocument } from './openapi-spec-utils';

function createBlockId(): string {
    return `block-${Date.now().toString(36)}-${Math.random().toString(36).slice(2, 9)}`;
}

function createHeadingBlock(title: string): Record<string, unknown> {
    return {
        id: createBlockId(),
        type: 'heading',
        props: { level: 1 },
        content: [{ type: 'text', text: title, styles: {} }],
        children: [],
    };
}

function createParagraphBlock(text: string): Record<string, unknown> {
    return {
        id: createBlockId(),
        type: 'paragraph',
        content: [{ type: 'text', text, styles: {} }],
        children: [],
    };
}

function descriptionToBlocks(text: string): Record<string, unknown>[] {
    if (looksLikeMarkdown(text)) {
        return markdownToBlocks(text) as Record<string, unknown>[];
    }

    return [createParagraphBlock(text)];
}

function createApiRefBlock(
    type:
        | 'graviteeApiOperations'
        | 'graviteeApiSchemas'
        | 'graviteeApiTryIt'
        | 'graviteeApiCodeSamples',
    tag: string,
): Record<string, unknown> {
    const baseProps = {
        tag,
        operationId: '',
    };

    switch (type) {
        case 'graviteeApiOperations':
            return {
                id: createBlockId(),
                type,
                props: {
                    ...baseProps,
                    showResponses: 'true',
                },
                children: [],
            };
        case 'graviteeApiSchemas':
            return {
                id: createBlockId(),
                type,
                props: baseProps,
                children: [],
            };
        case 'graviteeApiTryIt':
            return {
                id: createBlockId(),
                type,
                props: {
                    ...baseProps,
                    serverUrl: '',
                    authType: 'none',
                    authValue: '',
                },
                children: [],
            };
        case 'graviteeApiCodeSamples':
            return {
                id: createBlockId(),
                type,
                props: {
                    ...baseProps,
                    serverUrl: '',
                },
                children: [],
            };
        default:
            return {
                id: createBlockId(),
                type,
                props: baseProps,
                children: [],
            };
    }
}

export function createTagReferenceDocument(tag: string, tagDescription?: string): BlockNoteDocument {
    const descriptionBlocks = tagDescription
        ? descriptionToBlocks(tagDescription)
        : [createParagraphBlock(`Endpoints grouped under the ${tag} tag.`)];
    const blocks: Record<string, unknown>[] = [
        createHeadingBlock(tag),
        ...descriptionBlocks,
        createApiRefBlock('graviteeApiOperations', tag),
        createApiRefBlock('graviteeApiSchemas', tag),
        createApiRefBlock('graviteeApiTryIt', tag),
        createApiRefBlock('graviteeApiCodeSamples', tag),
    ];

    return blocks;
}

export function createOverviewReferenceDocument(apiName: string, apiDescription?: string): BlockNoteDocument {
    const introBlocks = apiDescription
        ? descriptionToBlocks(apiDescription)
        : [
              createParagraphBlock(
                  `Welcome to the ${apiName} API reference. Use the sidebar to browse endpoints grouped by tag.`,
              ),
          ];

    return [
        createHeadingBlock('Overview'),
        {
            id: createBlockId(),
            type: 'graviteeApiMetadata',
            props: { field: 'name' },
            children: [],
        },
        {
            id: createBlockId(),
            type: 'graviteeApiMetadata',
            props: { field: 'version' },
            children: [],
        },
        {
            id: createBlockId(),
            type: 'graviteeApiMetadata',
            props: { field: 'description' },
            children: [],
        },
        ...introBlocks,
    ];
}

export interface TagPageDefinition {
    readonly tag: string;
    readonly title: string;
    readonly description?: string;
    readonly document: BlockNoteDocument;
}

export async function buildTagPageDefinitions(apiId: string, apiName: string): Promise<{
    readonly overviewDocument: BlockNoteDocument;
    readonly tagPages: readonly TagPageDefinition[];
}> {
    const spec = await getOpenApiSpec(apiId);
    const parsed = parseOpenApiDocument(spec.content);
    const tags = parsed ? extractTags(parsed.document, parsed.operations) : [];

    const overviewDocument = createOverviewReferenceDocument(apiName, parsed?.document.info?.description);
    const tagPages = tags.map(tag => {
        const tagMeta = parsed?.document.tags?.find(item => item.name === tag);
        const document = createTagReferenceDocument(tag, tagMeta?.description);
        return {
            tag,
            title: tag,
            description: tagMeta?.description,
            document,
        };
    });

    return {
        overviewDocument,
        tagPages,
    };
}
