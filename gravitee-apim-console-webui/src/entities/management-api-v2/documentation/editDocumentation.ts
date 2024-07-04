/*
 * Copyright (C) 2015 The Gravitee team (http://gravitee.io)
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
import { Visibility } from './visibility';
import { PageType } from './pageType';
import { AccessControl } from './page';
import { PageSource } from './pageSource';

export interface BaseEditDocumentation {
  type?: PageType;
  name?: string;
  order?: number;
  visibility?: Visibility;
  accessControls?: AccessControl[];
  excludedAccessControls?: boolean;
}

export type EditDocumentationFolder = BaseEditDocumentation;

export interface EditDocumentationMarkdown extends BaseEditDocumentation {
  content?: string;
  homepage?: boolean;
  source?: PageSource;
}

export interface EditDocumentationSwagger extends BaseEditDocumentation {
  content?: string;
  homepage?: boolean;
  source?: PageSource;
}

export interface EditDocumentationAsyncApi extends BaseEditDocumentation {
  content?: string;
  homepage?: boolean;
  source?: PageSource;
}

export type EditDocumentation = EditDocumentationFolder | EditDocumentationMarkdown | EditDocumentationSwagger | EditDocumentationAsyncApi;
