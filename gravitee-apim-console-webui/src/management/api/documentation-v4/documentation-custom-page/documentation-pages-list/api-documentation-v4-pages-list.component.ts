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

import { Component, EventEmitter, Input, OnChanges, OnInit, Output, SimpleChanges } from '@angular/core';
import { MatTableDataSource } from '@angular/material/table';

import { getLogoForPageType, Page, getTooltipForPageType, getTitleForPageType } from '../../../../../entities/management-api-v2';

@Component({
  selector: 'api-documentation-v4-pages-list',
  templateUrl: './api-documentation-v4-pages-list.component.html',
  styleUrls: ['./api-documentation-v4-pages-list.component.scss'],
})
export class ApiDocumentationV4PagesListComponent implements OnInit, OnChanges {
  @Input()
  pages: Page[];

  @Input()
  isReadOnly: boolean;

  @Output()
  onEditPage = new EventEmitter<string>();

  @Output()
  onPublishPage = new EventEmitter<string>();

  @Output()
  onUnpublishPage = new EventEmitter<string>();

  @Output()
  onDeletePage = new EventEmitter<Page>();

  @Output()
  onGoToFolder = new EventEmitter<string>();

  @Output()
  onEditFolder = new EventEmitter<Page>();

  @Output()
  onMoveDown = new EventEmitter<Page>();

  @Output()
  onMoveUp = new EventEmitter<Page>();

  public displayedColumns = ['name', 'status', 'visibility', 'lastUpdated', 'actions'];
  public dataSource: MatTableDataSource<Page>;
  public pagesIncludeNonFolders: boolean;

  ngOnInit(): void {
    this.pagesIncludeNonFolders = this.pages.some((page) => page.type !== 'FOLDER');
  }

  ngOnChanges(changes: SimpleChanges): void {
    if (changes.pages) {
      this.dataSource = new MatTableDataSource<Page>(this.pages);
      this.ngOnInit();
    }
  }

  readonly getLogoForPageType = getLogoForPageType;
  readonly getTooltipForPageType = getTooltipForPageType;
  readonly getTitleForPageType = getTitleForPageType;
}
