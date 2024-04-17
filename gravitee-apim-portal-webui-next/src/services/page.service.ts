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
import { Injectable } from '@angular/core';

import { PageTreeNode } from '../components/page-tree/page-tree.component';
import { Page } from '../entities/page/page';

@Injectable({
  providedIn: 'root',
})
export class PageService {
  constructor() {}

  mapToPageTreeNode(root: string | undefined, pages: Page[]): PageTreeNode[] {
    return pages
      .filter(p => p.parent === root)
      .sort((p1, p2) => p1.order - p2.order)
      .map(p => ({
        id: p.id,
        name: p.name,
        children: this.mapToPageTreeNode(p.id, pages),
      }));
  }
}
