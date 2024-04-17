/*
 * Copyright (C) 2024 The Gravitee team (http://gravitee.io)
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

import { PageLinks } from './page-links';
import { PageMedia } from './page-media';
import { PageMetadata } from './page-metadata';
import { PageRevisionId } from './page-revision-id';

export interface Page {
  /**
   * Unique identifier of a page.
   */
  id: string;
  /**
   * Name of the page.
   */
  name: string;
  /**
   * Type of documentation.
   */
  type: PageTypeEnum;
  /**
   * Order of the documentation page in its folder.
   */
  order: number;
  /**
   * Parent page. MAY be null.
   */
  parent?: string;
  /**
   * Last update date and time.
   */
  updated_at?: Date;
  /**
   * list of media hash, attached to this page
   */
  media?: Array<PageMedia>;
  /**
   * Array of metadata about the page. This array is filled when the page has been fetched from a distant source (GitHub, GitLab, etc...).
   */
  metadata?: Array<PageMetadata>;
  _links?: PageLinks;
  /**
   * Only returned with (*)/apis/{apiId}/pages/{pageId}* and (*)/pages/{pageId}*. Need *include* query param to contain \'content\'.  The content of the page.
   */
  content?: string;
  contentRevisionId?: PageRevisionId;
}

export type PageTypeEnum = 'ASCIIDOC' | 'ASYNCAPI' | 'SWAGGER' | 'MARKDOWN' | 'FOLDER' | 'ROOT' | 'LINK';
