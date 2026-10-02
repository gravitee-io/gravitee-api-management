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
import { ComponentHarness } from '@angular/cdk/testing';

import { PageTreeHarness } from '../../../../components/page-tree/page-tree.harness';
import { PageTypeEnum } from '../../../../entities/page/page';

export class ApiDocumentationPagesHarness extends ComponentHarness {
  static readonly hostSelector = 'app-api-documentation-pages';

  private readonly locatePageTree = this.locatorForOptional(PageTreeHarness);
  private readonly locateEmptyState = this.locatorForOptional('[data-testid="api-documentation-pages-empty"]');
  private readonly locateError = this.locatorForOptional('[data-testid="api-documentation-pages-error"]');
  private readonly locateSwagger = this.locatorForOptional('app-page-swagger, app-page-redoc');
  private readonly locateMarkdown = this.locatorForOptional('app-page-markdown');
  private readonly locateAsciidoc = this.locatorForOptional('app-page-asciidoc');
  private readonly locateAsyncApi = this.locatorForOptional('app-page-async-api');

  async getPageTree(): Promise<PageTreeHarness | null> {
    return this.locatePageTree();
  }

  async getEmptyStateText(): Promise<string | null> {
    return (await this.locateEmptyState())?.text() ?? null;
  }

  async getErrorText(): Promise<string | null> {
    return (await this.locateError())?.text() ?? null;
  }

  async getDisplayedPageType(): Promise<PageTypeEnum | null> {
    if (await this.locateSwagger()) {
      return 'SWAGGER';
    }
    if (await this.locateMarkdown()) {
      return 'MARKDOWN';
    }
    if (await this.locateAsciidoc()) {
      return 'ASCIIDOC';
    }
    if (await this.locateAsyncApi()) {
      return 'ASYNCAPI';
    }
    return null;
  }
}
