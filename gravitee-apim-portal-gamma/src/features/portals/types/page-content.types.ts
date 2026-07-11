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
import type { BlockStyleOverrides } from '../../theming/types/block-styles.types';
import type { OpenApiRenderer } from './navigation-item.types';

/** Serialized BlockNote document (array of block objects). */
export type BlockNoteDocument = ReadonlyArray<Record<string, unknown>>;

export type PageContentType = 'BLOCK' | 'OPENAPI' | 'HTML' | 'ASYNCAPI';

interface BasePageContent {
    readonly id: string;
    readonly portalId: string;
    readonly navigationItemId: string;
    readonly contentType?: PageContentType;
}

export interface BlockPageContent extends BasePageContent {
    readonly contentType?: 'BLOCK';
    readonly document: BlockNoteDocument;
    /** GMD wire-format serialization of the document. */
    readonly gmd?: string;
    readonly blockStyles?: Record<string, BlockStyleOverrides>;
}

export interface OpenApiPageContent extends BasePageContent {
    readonly contentType: 'OPENAPI';
    readonly specContent: string;
    readonly renderer: OpenApiRenderer;
}

export interface HtmlPageContent extends BasePageContent {
    readonly contentType: 'HTML';
    readonly html: string;
    readonly css?: string;
    /** When true, constrain content to the portal layout page width. Defaults to full width. */
    readonly followLayoutWidth?: boolean;
}

export interface AsyncApiPageContent extends BasePageContent {
    readonly contentType: 'ASYNCAPI';
    readonly specContent: string;
}

export type PageContent = BlockPageContent | OpenApiPageContent | HtmlPageContent | AsyncApiPageContent;
