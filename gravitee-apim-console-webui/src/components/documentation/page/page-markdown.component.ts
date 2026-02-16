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

import { marked } from 'marked';

export class PageMarkdownController implements ng.IComponentController, ng.IOnInit {
  constructor(private readonly $sanitize: ng.sanitize.ISanitizeService) {}

  page: any;
  htmlContent: string;

  $onInit(): void {
    Promise.resolve(marked.parse(this.page?.content ?? '')).then(html => {
      this.htmlContent = this.$sanitize(html);
    });
  }
}
PageMarkdownController.$inject = ['$sanitize'];

export const PageMarkdownComponent: ng.IComponentOptions = {
  template: require('html-loader!./page-markdown.html').default, // eslint-disable-line @typescript-eslint/no-var-requires
  bindings: {
    page: '<',
  },
  controller: PageMarkdownController,
};
