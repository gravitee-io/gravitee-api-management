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
import { Page } from './page';

const BASE_PAGE: Page = {
  id: 'my-first-page',
  name: 'My first page',
  crossId: 'my-cross-id',
  order: 1,
  lastContributor: 'current user',
  published: true,
  visibility: 'PUBLIC',
  updatedAt: new Date(),
  parentId: null,
};

export function fakeMarkdown(modifier?: Partial<Page>): Page {
  const markdownModifier: Page = {
    content: 'my markdown content',
    type: 'MARKDOWN',
  };

  return {
    ...BASE_PAGE,
    ...markdownModifier,
    ...modifier,
  };
}

export function fakeFolder(modifier?: Partial<Page>): Page {
  const folderModifier: Page = {
    type: 'FOLDER',
  };

  return {
    ...BASE_PAGE,
    ...folderModifier,
    ...modifier,
  };
}
