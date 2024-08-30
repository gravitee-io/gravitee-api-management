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
import {
  ChangeDetectionStrategy,
  Component,
  EventEmitter,
  Injectable,
  Input,
  Output,
  signal,
  OnChanges,
  SimpleChanges,
} from '@angular/core';
import { MatTreeModule, MatTree, MatTreeNode, MatTreeNodePadding, MatTreeNodeToggle } from '@angular/material/tree';
import { MatIcon } from '@angular/material/icon';
import { MatIconButton } from '@angular/material/button';
import { FlatTreeControl } from '@angular/cdk/tree';
import { MatProgressBar } from '@angular/material/progress-bar';
import { map } from 'rxjs/operators';
import { BehaviorSubject, merge, Observable } from 'rxjs';
import { CollectionViewer, DataSource, SelectionChange } from '@angular/cdk/collections';
import { MatRadioModule } from '@angular/material/radio';
import { FormsModule } from '@angular/forms';
import { NgIf, NgOptimizedImage, TitleCasePipe } from '@angular/common';
import { MatTooltip } from '@angular/material/tooltip';

import { getLogoForPageType, getTitleForPageType, getTooltipForPageType, Page } from '../../../../../entities/management-api-v2';

export class PageFlatNode {
  constructor(
    public id: string,
    public name: string,
    public type: string,
    public published: boolean,
    public visibility: string,
    public generalConditions: boolean,
    public level = 1,
    public expandable = false,
    public logoForPageType: string,
    public titleForPageType: string,
    public tooltipForPageType: string,
    public isLoading = signal(false),
  ) {}
}

@Injectable({ providedIn: 'root' })
export class PageData {
  private pages: Page[] = [];

  setPages(pages: Page[]): void {
    this.pages = pages;
  }

  initialData(): PageFlatNode[] {
    if (!this.pages) {
      return [];
    }
    return this.pages.filter((page) => !page.parentId).map((page) => this.getPageFlatNode(page));
  }

  getPageFlatNode(page: Page, level: number = 0) {
    const logoForPageType = getLogoForPageType(page.type);
    const titleForPageType = getTitleForPageType(page.type);
    const tooltipForPageType = getTooltipForPageType(page.type);
    return new PageFlatNode(
      page.id,
      page.name,
      page.type,
      page.published,
      page.visibility,
      page.generalConditions,
      level,
      page.type === 'FOLDER',
      logoForPageType ? logoForPageType.toString() : '',
      titleForPageType ? titleForPageType.toLowerCase() + ' logo' : '',
      tooltipForPageType,
    );
  }

  getChildren(parentId: string): Page[] {
    return this.pages.filter((page) => page.parentId === parentId);
  }

  isExpandable(page: Page): boolean {
    return page.type === 'FOLDER';
  }
}

export class PageDataSource implements DataSource<PageFlatNode> {
  dataChange = new BehaviorSubject<PageFlatNode[]>([]);
  private _collectionViewer: CollectionViewer;

  get data(): PageFlatNode[] {
    return this.dataChange.value;
  }

  set data(value: PageFlatNode[]) {
    this._treeControl.dataNodes = value;
    this.dataChange.next(value);
  }

  constructor(
    private _treeControl: FlatTreeControl<PageFlatNode>,
    private _pageData: PageData,
  ) {}

  connect(collectionViewer: CollectionViewer): Observable<PageFlatNode[]> {
    this._treeControl.expansionModel.changed.subscribe((change) => {
      if ((change as SelectionChange<PageFlatNode>).added || (change as SelectionChange<PageFlatNode>).removed) {
        this.handleTreeControl(change as SelectionChange<PageFlatNode>);
      }
    });

    return merge(collectionViewer.viewChange, this.dataChange).pipe(map(() => this.data));
  }

  disconnect(collectionViewer: CollectionViewer): void {
    this._collectionViewer = collectionViewer;
  }

  handleTreeControl(change: SelectionChange<PageFlatNode>) {
    if (change.added) {
      change.added.forEach((node) => this.toggleNode(node, true));
    }
    if (change.removed) {
      change.removed
        .slice()
        .reverse()
        .forEach((node) => this.toggleNode(node, false));
    }
  }

  toggleNode(node: PageFlatNode, expand: boolean) {
    const children = this._pageData.getChildren(node.id);
    const index = this.data.indexOf(node);
    if (!children || index < 0) {
      return;
    }

    node.isLoading.set(true);

    setTimeout(() => {
      if (expand) {
        const nodes = children.map((page) => this._pageData.getPageFlatNode(page, node.level + 1));
        this.data.splice(index + 1, 0, ...nodes);
      } else {
        const count = 0;
        this.data.splice(index + 1, count);
      }

      this.dataChange.next(this.data);
      node.isLoading.set(false);
    }, 1000);
  }
}

@Component({
  selector: 'api-documentation-choose-page-list',
  templateUrl: './api-documentation-choose-page-list.component.html',
  styleUrl: './api-documentation-choose-page-list.component.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [
    MatIcon,
    MatTreeNode,
    MatTree,
    MatProgressBar,
    MatTreeNodePadding,
    MatTreeNodeToggle,
    MatIconButton,
    MatTreeModule,
    FormsModule,
    MatRadioModule,
    NgOptimizedImage,
    MatTooltip,
    NgIf,
    TitleCasePipe,
  ],
  standalone: true,
})
export class ApiDocumentationChoosePageListComponent implements OnChanges {
  @Input() pages: Page[];
  @Output() selectPage = new EventEmitter<string>();

  selectedPageId: string;
  treeControl: FlatTreeControl<PageFlatNode>;
  dataSource: PageDataSource;

  constructor(private pageData: PageData) {
    this.treeControl = new FlatTreeControl<PageFlatNode>(this.getLevel, this.isExpandable);
    this.dataSource = new PageDataSource(this.treeControl, pageData);
  }

  ngOnChanges(changes: SimpleChanges): void {
    if (changes.pages) {
      this.pageData.setPages(this.pages);
      this.dataSource.data = this.pageData.initialData();
    }
  }

  getLevel = (node: PageFlatNode) => node.level;

  isExpandable = (node: PageFlatNode) => node.expandable;

  hasChild = (_: number, _nodeData: PageFlatNode) => _nodeData.expandable;

  onSelectPage(node: PageFlatNode) {
    this.selectPage.emit(node.id);
  }
}
