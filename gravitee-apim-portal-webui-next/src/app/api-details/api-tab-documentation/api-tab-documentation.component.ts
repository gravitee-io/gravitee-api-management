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
import { AsyncPipe } from '@angular/common';
import { Component, Input, OnInit } from '@angular/core';

import { PageTreeComponent, PageTreeNode } from '../../../components/page-tree/page-tree.component';
import { Page } from '../../../entities/page/page';
import { PageService } from '../../../services/page.service';

@Component({
  selector: 'app-api-tab-documentation',
  standalone: true,
  imports: [PageTreeComponent, AsyncPipe],
  templateUrl: './api-tab-documentation.component.html',
  styleUrl: './api-tab-documentation.component.scss',
})
export class ApiTabDocumentationComponent implements OnInit {
  @Input()
  pages: Page[] = [];
  pageNodes: PageTreeNode[] = [];
  selectedPage: string = '';

  constructor(private pageService: PageService) {}

  ngOnInit(): void {
    this.pageNodes = this.pageService.mapToPageTreeNode(undefined, this.pages);
  }

  showPage(pageId: string) {
    this.selectedPage = pageId;
  }
}
