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
import { FlatTreeControl } from '@angular/cdk/tree';
import { NgClass } from '@angular/common';
import { Component, EventEmitter, Input, OnChanges, OnInit, Output, SimpleChanges } from '@angular/core';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatTreeFlatDataSource, MatTreeFlattener, MatTreeModule } from '@angular/material/tree';
import { isEmpty } from 'lodash';

export interface PageTreeNode {
  id: string;
  name: string;
  isFolder?: boolean;
  order?: number;
  children?: PageTreeNode[];
}

/** Flat node with expandable and level information */
interface FlatNode {
  expandable: boolean;
  name: string;
  level: number;
  id: string;
}

@Component({
  selector: 'app-page-tree',
  standalone: true,
  imports: [MatTreeModule, MatButtonModule, MatIconModule, NgClass],
  templateUrl: './page-tree.component.html',
  styleUrl: './page-tree.component.scss',
})
export class PageTreeComponent implements OnInit, OnChanges {
  @Input({ required: true })
  pages!: PageTreeNode[];

  @Input()
  activePage: string | undefined;

  @Output()
  openFile = new EventEmitter<string>();
  dataSource: MatTreeFlatDataSource<PageTreeNode, FlatNode>;
  treeControl: FlatTreeControl<FlatNode>;
  selectedNode: string = '';

  private readonly treeFlattener: MatTreeFlattener<PageTreeNode, FlatNode>;

  constructor() {
    this.treeFlattener = new MatTreeFlattener(
      this._transformer,
      node => node.level,
      node => node.expandable,
      node => node.children,
    );
    this.treeControl = new FlatTreeControl<FlatNode>(
      node => node.level,
      node => node.expandable,
    );
    this.dataSource = new MatTreeFlatDataSource(this.treeControl, this.treeFlattener);
  }

  ngOnInit() {
    this.dataSource.data = this.pages;
    this.treeControl.expandAll();
  }

  ngOnChanges(changes: SimpleChanges): void {
    if (changes['activePage']) {
      const activePage = changes['activePage'].currentValue;
      if (activePage) {
        this.selectedNode = activePage;
      } else if (!isEmpty(this.pages)) {
        this.fileSelected(this.getFirstAvailablePage(this.pages[0]).id);
      }
    }
  }

  hasChild = (_: number, node: FlatNode) => node.expandable;

  fileSelected(id: string) {
    this.selectedNode = id;
    this.openFile.emit(id);
  }

  private _transformer = (node: PageTreeNode, level: number): FlatNode => ({
    expandable: !!node.children && node.children.length > 0,
    name: node.name,
    level: level,
    id: node.id,
  });

  private getFirstAvailablePage(node: PageTreeNode): PageTreeNode {
    if (node?.children && node.children.length > 0) {
      return this.getFirstAvailablePage(node.children[0]);
    }
    return node;
  }
}
