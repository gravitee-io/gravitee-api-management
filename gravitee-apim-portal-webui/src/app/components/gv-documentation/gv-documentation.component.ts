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
import { AfterViewInit, Component, HostListener, Input, ViewChild } from '@angular/core';
import { Page } from '../../../../projects/portal-webclient-sdk/src/lib';
import { TreeItem } from '../../model/tree-item';
import { NotificationService } from '../../services/notification.service';
import { ActivatedRoute, Router } from '@angular/router';
import '@gravitee/ui-components/wc/gv-tree';
import { animate, style, transition, trigger } from '@angular/animations';
import { ScrollService } from '../../services/scroll.service';

@Component({
  selector: 'app-gv-documentation',
  templateUrl: './gv-documentation.component.html',
  styleUrls: ['./gv-documentation.component.css'],
  animations: [
    trigger('grow', [
      transition('void <=> *', []),
      transition('* <=> *', [style({ height: '{{startHeight}}px', opacity: 0 }), animate('.5s ease')], { params: { startHeight: 0 } }),
    ]),
  ],
})
export class GvDocumentationComponent implements AfterViewInit {
  @Input() set pages(pages: Page[]) {
    clearTimeout(this.loadingTimer);
    if (pages && pages.length) {
      if (this._pages !== pages) {
        this._pages = pages;
        let pageToDisplay;
        const pageId = this.route.snapshot.queryParams.page;
        const folderId = this.route.snapshot.queryParams.folder;
        if (pageId) {
          pageToDisplay = this.getFirstPage(pages, pageId);
        } else if (folderId) {
          const folderPages = pages.filter((p) => p.parent === folderId);
          pageToDisplay = this.getFirstPage(folderPages);
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
    this.loadingTimer = setTimeout(() => {
      this.isLoaded = true;
    }, 700);
  }

  constructor(private notificationService: NotificationService, private route: ActivatedRoute, private router: Router) {}

  static PAGE_PADDING_TOP_BOTTOM = 44;
  static PAGE_COMPONENT = 'app-gv-page';

  currentPage: Page;
  currentMenuItem: TreeItem;
  menu: TreeItem[];
  isLoaded = false;
  hasTreeClosed = false;

  @Input() rootDir: string;
  private _pages: Page[];

  @ViewChild('treeMenu', { static: false }) treeMenu;
  private loadingTimer: any;

  @Input() fragment: string;

  static updateMenuHeight(menuElement) {
    if (menuElement) {
      const viewportHeight = Math.max(document.documentElement.clientHeight, window.innerHeight || 0);
      menuElement.style.height = `${
        viewportHeight - (ScrollService.getHeaderHeight() + 2 * GvDocumentationComponent.PAGE_PADDING_TOP_BOTTOM)
      }px`;
    }
  }

  static updateMenuPosition(menuElement) {
    if (menuElement) {
      const scrollTop = document.scrollingElement.scrollTop;
      if (document.querySelector(this.PAGE_COMPONENT)) {
        const contentHeight = document.querySelector(this.PAGE_COMPONENT).getBoundingClientRect().height;

        menuElement.style['max-height'] = `${contentHeight - scrollTop}px`;

        this.reset(menuElement);
      } else {
        this.reset(menuElement);
      }
    }
  }

  static reset(menuElement) {
    if (menuElement) {
      const top = ScrollService.getHeaderHeight() + GvDocumentationComponent.PAGE_PADDING_TOP_BOTTOM;
      menuElement.style.bottom = `${GvDocumentationComponent.PAGE_PADDING_TOP_BOTTOM}px`;
      menuElement.style.top = `${top}px`;
      menuElement.style.position = `fixed`;
      this.updateMenuHeight(menuElement);
    }
  }

  private initTree(pages: Page[], selectedPage?: string) {
    let pagesMap: any[] = pages;
    pagesMap.forEach((page) => {
      if (page.parent) {
        const parentPage = pagesMap.find((element) => element.id === page.parent);
        if (parentPage) {
          if (parentPage.children) {
            parentPage.children.push(page);
          } else {
            parentPage.children = [page];
          }
        }
      }
    });
    pagesMap = pagesMap
      .filter((page) => (!page.parent && page.type.toUpperCase() !== Page.TypeEnum.ROOT) || (page.parent && page.parent === this.rootDir))
      .sort((p1, p2) => p1.order - p2.order);
    return this.buildMenu(pagesMap, selectedPage);
  }

  private buildMenu(pages: any[], selectedPage?: string) {
    const result: TreeItem[] = [];
    pages.forEach((page) => {
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
        parents.forEach((parent) => (parent.expanded = true));
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

  ngAfterViewInit(): void {
    setTimeout(() => {
      GvDocumentationComponent.reset(this.treeMenu.nativeElement);
    }, 0);
  }

  @HostListener('window:resize')
  onResize() {
    if (this.treeMenu) {
      window.requestAnimationFrame(() => {
        GvDocumentationComponent.updateMenuHeight(this.treeMenu.nativeElement);
      });
    }
  }

  @HostListener('window:scroll')
  onScroll() {
    if (this.treeMenu) {
      window.requestAnimationFrame(() => {
        GvDocumentationComponent.updateMenuPosition(this.treeMenu.nativeElement);
      });
    }
  }

  @HostListener(':gv-tree:select', ['$event.detail.value'])
  onPageChange(page) {
    this.router.navigate([], { queryParams: { page: page.id } }).then(() => {
      GvDocumentationComponent.reset(this.treeMenu.nativeElement);
    });
    this.currentPage = page;
    this.currentMenuItem = this.findMenuItem(this.menu, page);
  }

  @HostListener(':gv-tree:toggle', ['$event.detail.closed'])
  onToggleTree(closed) {
    this.hasTreeClosed = closed;
  }

  isAsciiDoc(page: Page) {
    return page && page.type.toUpperCase() === Page.TypeEnum.ASCIIDOC;
  }

  isAsyncApi(page: Page) {
    return page && page.type.toUpperCase() === Page.TypeEnum.ASYNCAPI;
  }

  isMarkdown(page: Page) {
    return page && page.type.toUpperCase() === Page.TypeEnum.MARKDOWN;
  }

  isSwagger(page: Page) {
    return page && page.type.toUpperCase() === Page.TypeEnum.SWAGGER;
  }

  private getFirstPage(pages: any[], pageId?: string) {
    return pages
      .filter((page) => this.isAsciiDoc(page) || this.isAsyncApi(page) || this.isSwagger(page) || this.isMarkdown(page))
      .find((page) =>
        // Check pageId if input is defined otherwise fallback on the first page
        pageId ? pageId === page.id : true,
      );
  }

  isEmpty() {
    return this.isLoaded && (!this.menu || this.menu.length === 0);
  }

  @HostListener(':gv-button:click', ['$event.srcElement.dataset.pageId'])
  onInternalLinkClick(pageId: string) {
    if (pageId) {
      const pageToDisplay = this._pages.find((page) => page.id === pageId);
      this.onPageChange(pageToDisplay);
    }
  }

  private findMenuItem(menu: TreeItem[], pageToFind: any) {
    if (menu) {
      for (const item of menu) {
        if (item.value === pageToFind) {
          return item;
        }
        if (item.children && item.children.length > 0) {
          const foundItem = this.findMenuItem(item.children, pageToFind);
          if (foundItem) {
            return foundItem;
          }
        }
      }
    }
    return null;
  }
}
