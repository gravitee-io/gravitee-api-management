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
import { Component } from '@angular/core';
import { toSignal } from '@angular/core/rxjs-interop';
import { MatCard, MatCardContent } from '@angular/material/card';
import { ActivatedRoute, Router, RouterOutlet } from '@angular/router';
import { map, startWith, switchMap } from 'rxjs';

import { LoaderComponent } from '../../components/loader/loader.component';
import { PageTreeComponent } from '../../components/page-tree/page-tree.component';
import { PageService } from '../../services/page.service';

@Component({
  selector: 'app-guides',
  standalone: true,
  imports: [LoaderComponent, PageTreeComponent, MatCard, MatCardContent, RouterOutlet],
  templateUrl: './guides.component.html',
  styleUrls: ['./guides.component.scss'],
})
export class GuidesComponent {
  protected pagesData = toSignal(
    this.pageService.listByEnvironment().pipe(
      map(pagesResponse => {
        const pages = (pagesResponse.data ?? []).filter(p => p.type !== 'LINK');
        const nodes = this.pageService.mapToPageTreeNode(undefined, pages);
        return { pages, nodes };
      }),
    ),
  );
  protected pageId = toSignal(
    this.activatedRoute.url.pipe(
      startWith(null),
      switchMap(() => this.activatedRoute.firstChild?.paramMap ?? []),
      map(params => params.get('pageId')),
    ),
    { initialValue: null },
  );

  constructor(
    private readonly pageService: PageService,
    private readonly router: Router,
    private readonly activatedRoute: ActivatedRoute,
  ) {}

  showPage(pageId: string): void {
    this.router.navigate(['.', pageId], { relativeTo: this.activatedRoute });
  }
}
