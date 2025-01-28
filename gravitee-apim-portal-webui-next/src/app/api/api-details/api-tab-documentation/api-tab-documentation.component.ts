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
import { NgClass } from '@angular/common';
import { Component, Input, OnInit, signal, WritableSignal } from '@angular/core';
import { MatSidenavModule } from '@angular/material/sidenav';
import { ActivatedRoute, Router, RouterModule } from '@angular/router';
import { Observable, of } from 'rxjs';

import { ApiDocumentationComponent } from './components/api-documentation/api-documentation.component';
import { DrawerComponent } from '../../../../components/drawer/drawer.component';
import { PageTreeComponent, PageTreeNode } from '../../../../components/page-tree/page-tree.component';
import { Page } from '../../../../entities/page/page';
import { PageService } from '../../../../services/page.service';

interface SelectedPageData {
  result?: Page;
  error?: unknown;
}

@Component({
  selector: 'app-api-tab-documentation',
  standalone: true,
  imports: [PageTreeComponent, RouterModule, ApiDocumentationComponent, MatSidenavModule, DrawerComponent, NgClass],
  templateUrl: './api-tab-documentation.component.html',
  styleUrl: './api-tab-documentation.component.scss',
})
export class ApiTabDocumentationComponent implements OnInit {
  @Input()
  apiId!: string;
  @Input()
  pages!: Page[];
  pageNodes: PageTreeNode[] = [];
  selectedPageData$: Observable<SelectedPageData> = of();
  pageId = signal<string | undefined>(undefined);
  isSidebarExpanded: WritableSignal<boolean> = signal(true);

  constructor(
    private readonly pageService: PageService,
    private readonly router: Router,
    private readonly activatedRoute: ActivatedRoute,
  ) {}

  ngOnInit() {
    this.pageNodes = this.pageService.mapToPageTreeNode(undefined, this.pages);
    if (this.pageNodes.length == 1) {
      this.isSidebarExpanded.set(false);
    }
  }

  showPage(pageId: string) {
    this.pageId.set(pageId);
    this.router.navigate(['.', pageId], { relativeTo: this.activatedRoute });
  }
}
