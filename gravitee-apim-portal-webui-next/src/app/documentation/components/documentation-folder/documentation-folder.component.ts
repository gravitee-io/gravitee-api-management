/*
 * Copyright (C) 2025 The Gravitee team (http://gravitee.io)
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
import { Component, effect, input, signal } from '@angular/core';
import { toSignal } from '@angular/core/rxjs-interop';
import { ActivatedRoute, Router } from '@angular/router';
import { map } from 'rxjs';

import { GraviteeMarkdownViewerModule } from '@gravitee/gravitee-markdown';

import { TreeComponent } from './tree-component/tree.component';
import { InnerLinkDirective } from '../../../../directives/inner-link.directive';
import { MobileClassDirective } from '../../../../directives/mobile-class.directive';
import { PortalNavigationItem } from '../../../../entities/portal-navigation/portal-navigation-item';
import { PortalNavigationItemsService } from '../../../../services/portal-navigation-items.service';

@Component({
  selector: 'app-documentation-folder',
  imports: [MobileClassDirective, TreeComponent, GraviteeMarkdownViewerModule, InnerLinkDirective],
  standalone: true,
  templateUrl: './documentation-folder.component.html',
  styleUrl: './documentation-folder.component.scss',
})
export class DocumentationFolderComponent {
  navItem = input<PortalNavigationItem | null>(null);
  children = signal<PortalNavigationItem[] | null>(null);
  pageId = toSignal<string>(this.activatedRoute.queryParams.pipe(map(params => params['pageId'])), { initialValue: null });
  selectedPageContent = signal<string>('');

  constructor(
    private router: Router,
    private activatedRoute: ActivatedRoute,
    private itemsService: PortalNavigationItemsService,
  ) {
    effect(() => this.loadChildren());
    effect(() => this.loadPageContent());
  }

  onSelect(selectedPageId: string | null) {
    if (selectedPageId) {
      this.router.navigate([], {
        relativeTo: this.activatedRoute,
        queryParams: { pageId: selectedPageId },
      });
    }
  }

  private loadChildren() {
    this.children.set(null);

    const navItem = this.navItem();
    if (!navItem) {
      // nothing to do
      return;
    }

    this.itemsService.getNavigationItems('TOP_NAVBAR', true, navItem.id).subscribe(data => this.children.set(data));
  }

  private loadPageContent() {
    const children = this.children();
    if (!children) {
      this.selectedPageContent.set('');
      return;
    }

    const pageId = this.pageId();
    if (!pageId) {
      // nothing to do
      return;
    }

    const pageExistsInChildren = children.find(item => item.id === pageId);
    if (!pageExistsInChildren) {
      setTimeout(() => this.router.navigate(['/404']));
      return;
    }

    this.itemsService.getNavigationItemContent(pageId).subscribe(content => this.selectedPageContent.set(content));
  }
}
