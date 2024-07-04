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
<<<<<<< HEAD
=======
import { AccessControl } from './page';
import { PageSource } from './pageSource';
>>>>>>> 2eb1d7f2cd (feat(console): User can import and publish a page from a remote URL)

const CreateDocumentationTypeEnum = {
  MARKDOWN: 'MARKDOWN',
  SWAGGER: 'SWAGGER',
  ASYNCAPI: 'ASYNCAPI',
  FOLDER: 'FOLDER',
} as const;
export type CreateDocumentationType = (typeof CreateDocumentationTypeEnum)[keyof typeof CreateDocumentationTypeEnum];

export interface BaseCreateDocumentation {
  name: string;
  type: CreateDocumentationType;
  order?: number;
  visibility?: Visibility;
  parentId?: string;
}

export type CreateDocumentationFolder = BaseCreateDocumentation;

export interface CreateDocumentationMarkdown extends BaseCreateDocumentation {
  content?: string;
  homepage?: boolean;
  source?: PageSource;
}

export interface CreateDocumentationSwagger extends BaseCreateDocumentation {
  content?: string;
  homepage?: boolean;
  source?: PageSource;
}

export interface CreateDocumentationAsyncApi extends BaseCreateDocumentation {
  content?: string;
  homepage?: boolean;
  source?: PageSource;
}

export type CreateDocumentation =
  | ({ type: 'FOLDER' } & CreateDocumentationFolder)
  | ({ type: 'MARKDOWN' } & CreateDocumentationMarkdown)
  | ({ type: 'SWAGGER' } & CreateDocumentationSwagger)
  | ({ type: 'ASYNCAPI' } & CreateDocumentationAsyncApi);
