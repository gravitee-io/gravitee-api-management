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
import { IComponentControllerService } from 'angular';

import { PageMarkdownController } from './page-markdown.component';

import { setupAngularJsTesting } from '../../../../old-jest.setup.js';

setupAngularJsTesting();

describe('PageMarkdownController', () => {
  let $componentController: IComponentControllerService;
  let pageMarkdownComponent: PageMarkdownController;

  beforeEach(inject(_$componentController_ => {
    $componentController = _$componentController_;
    pageMarkdownComponent = $componentController('gvPageMarkdown', null, {});
    pageMarkdownComponent.page = {
      name: 'A Page',
      id: '86de4f08-aa02-40f0-aa73-4b3e0e97fef4',
      content: '# Hello world',
      type: 'MARKDOWN',
      order: 1,
    };
    pageMarkdownComponent.$onInit();
  }));

  describe('$onInit', () => {
    it('convert Markdown content into HTML', () => {
      expect(pageMarkdownComponent.htmlContent).toEqual('<h1>Hello world</h1>&#10;');
    });

    it('sanitize HTML content to avoid XSS', () => {
      pageMarkdownComponent.page = {
        name: 'A Page',
        id: '86de4f08-aa02-40f0-aa73-4b3e0e97fef4',
        content: '[Click me](javascript:alert("XSS"))',
        type: 'MARKDOWN',
        order: 1,
      };
      pageMarkdownComponent.$onInit();
      expect(pageMarkdownComponent.htmlContent).toEqual('<p><a>Click me</a></p>&#10;');
    });
  });
});
