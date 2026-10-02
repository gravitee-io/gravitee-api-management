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
import { Component, computed, inject, input, linkedSignal } from '@angular/core';
import { rxResource } from '@angular/core/rxjs-interop';
import { map } from 'rxjs';

import { DocumentationSkeletonComponent } from '../../../../components/documentation-skeleton/documentation-skeleton.component';
import { PageComponent } from '../../../../components/page/page.component';
import { PageTreeComponent, PageTreeNode } from '../../../../components/page-tree/page-tree.component';
import { Page } from '../../../../entities/page/page';
import { PageService } from '../../../../services/page.service';

interface SelectedPageParams {
  apiId: string;
  pageId: string;
}

/**
 * Shows the published pages of an API (OpenAPI/AsyncAPI specifications, Markdown, AsciiDoc), as the classic portal does.
 * Used for an API navigation item that has no navigation pages of its own.
 */
@Component({
  selector: 'app-api-documentation-pages',
  imports: [DocumentationSkeletonComponent, PageComponent, PageTreeComponent],
  templateUrl: './api-documentation-pages.component.html',
  styleUrl: './api-documentation-pages.component.scss',
})
export class ApiDocumentationPagesComponent {
  private readonly pageService = inject(PageService);

  apiId = input.required<string>();

  pages = rxResource<Page[], string>({
    params: () => this.apiId(),
    stream: ({ params }) => this.pageService.listByApiId(params).pipe(map(({ data }) => data ?? [])),
  });
  pageTree = computed<PageTreeNode[]>(() =>
    this.pages.hasValue() ? this.pageService.mapToPageTreeNode(undefined, this.pages.value()) : [],
  );
  hasSeveralPages = computed(() => this.pageTree().length > 1 || !!this.pageTree()[0]?.children?.length);
  selectedPageId = linkedSignal<string | undefined>(() => findFirstPageId(this.pageTree()));
  selectedPage = rxResource<Page, SelectedPageParams | undefined>({
    params: () => {
      const pageId = this.selectedPageId();
      return pageId ? { apiId: this.apiId(), pageId } : undefined;
    },
    stream: ({ params }) => this.pageService.getByApiIdAndId(params.apiId, params.pageId, true),
  });
}

const findFirstPageId = (nodes: PageTreeNode[]): string | undefined => {
  const [first] = nodes;
  if (!first) {
    return undefined;
  }
  return first.children?.length ? findFirstPageId(first.children) : first.id;
};
