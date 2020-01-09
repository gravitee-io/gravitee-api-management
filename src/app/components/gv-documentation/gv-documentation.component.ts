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
import { Component, HostListener, Input, OnInit } from '@angular/core';
import { Page } from '@gravitee/ng-portal-webclient';
import { TreeItem } from '../../model/tree-item';
import { NotificationService } from '../../services/notification.service';
import { ActivatedRoute, Router } from '@angular/router';

@Component({
  selector: 'app-gv-documentation',
  templateUrl: './gv-documentation.component.html',
  styleUrls: ['./gv-documentation.component.css']
})
export class GvDocumentationComponent implements OnInit {

  currentPage: Page;
  currentMenuItem: TreeItem;
  menu: TreeItem[];

  @Input() set pages(pages: Page[]) {
    if (pages && pages.length) {
      let pageToDisplay;
      const pageId = this.route.snapshot.queryParams.page;
      if (pageId) {
        pageToDisplay = this.getFirstPage(pages, pageId);
      } else {
        pageToDisplay = this.getFirstPage(pages);
      }
      if (pageToDisplay) {
        this.onPageChange(pageToDisplay);
        this.menu = this.initTree(pages, pageToDisplay.id);
        this.expandMenu(this.menu);
      } else {
        this.menu = [];
      }
    }
  }

  constructor(
    private notificationService: NotificationService,
    private route: ActivatedRoute,
    private router: Router,
    ) { }

  ngOnInit() {
  }

  private initTree(pages, selectedPage?: string) {
    let pagesMap: any = pages;
    pagesMap.forEach(page => {
      if (page.parent) {
        const parentPage = pagesMap.find(element => element.id === page.parent);
        if (parentPage) {
          if (parentPage.children) {
            parentPage.children.push(page);
          } else {
            parentPage.children = [page];
          }
        }
      }
    });

    pagesMap = pagesMap.filter(page => !page.parent && page.type.toUpperCase() !== Page.TypeEnum.ROOT).sort(page => page.order);
    return this.buildMenu(pagesMap, selectedPage);
  }

  private buildMenu(pages: any[], selectedPage?: string) {
    const result: TreeItem[] = [];
    pages.forEach(page => {
      const name = page.name;
      let treeItem;
      if (page.children) {
        treeItem = new TreeItem(name, page, this.buildMenu(page.children, selectedPage));
      } else {
        treeItem = new TreeItem(name, page, []);
      }
      result.push(treeItem);
      if (selectedPage && selectedPage === page.id) {
        this.currentMenuItem = treeItem;
      }
    });
    return result;
  }

  expandMenu(menu: TreeItem[], parents?: TreeItem[], firstLevel: boolean = true) {
    menu.forEach((menuItem) => {
      if (menuItem === this.currentMenuItem && parents) {
        parents.forEach(parent => parent.expanded = true);
      } else {
        if (menuItem.children) {
          if (parents && !firstLevel) {
            parents.push(menuItem);
          } else {
            parents = [menuItem];
          }
          this.expandMenu(menuItem.children, parents, false);
        }
      }
    });
  }

  @HostListener(':gv-tree:select', ['$event.detail.value'])
  onPageChange(page) {
    this.router.navigate([], { queryParams: { page: page.id } });
    this.currentPage = page;
  }

  isMarkdown(page: Page) {
    return page && page.type.toUpperCase() === Page.TypeEnum.MARKDOWN;
  }

  isSwagger(page: Page) {
    return page && page.type.toUpperCase() === Page.TypeEnum.SWAGGER;
  }

  private getFirstPage(pages: any[], pageId?: string) {
    for (const page of pages) {
      if (this.isSwagger(page) || this.isMarkdown(page)) {
        if (pageId) {
          if (pageId === page.id) {
            return page;
          }
        } else {
          return page;
        }
      }
    }
  }

  isEmpty() {
    return !this.menu || this.menu.length === 0;
  }
}
