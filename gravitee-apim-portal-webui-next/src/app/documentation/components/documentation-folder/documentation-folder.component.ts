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
import {ChangeDetectionStrategy, Component, computed, effect, Input, input, model, Signal, signal} from '@angular/core';
import {MobileClassDirective} from "../../../../directives/mobile-class.directive";
import {MatCard} from "@angular/material/card";
import {SectionNode, TreeComponent} from "./tree-component/tree.component";
import {
  PortalNavigationItem,
  PortalNavigationPage
} from "../../../../entities/portal-navigation/portal-navigation-item";
import {BehaviorSubject, catchError, forkJoin, from, map, Observable, pipe, switchMap, tap} from "rxjs";
import {ActivatedRoute, Router} from "@angular/router";
import {PortalNavigationItemsService} from "../../../../services/portal-navigation-items.service";
import {toObservable, toSignal} from "@angular/core/rxjs-interop";
import {AsyncPipe} from "@angular/common";
import {GraviteeMarkdownViewerModule} from "@gravitee/gravitee-markdown";
import {InnerLinkDirective} from "../../../../directives/inner-link.directive";
import {of} from "rxjs/internal/observable/of";
import {matBottomSheetAnimations} from "@angular/material/bottom-sheet";

@Component({
  selector: 'app-documentation-folder',
  imports: [MobileClassDirective, TreeComponent, AsyncPipe, MatCard, TreeComponent, GraviteeMarkdownViewerModule, InnerLinkDirective],
  standalone: true,
  templateUrl: "./documentation-folder.component.html",
  styleUrl: "./documentation-folder.component.scss",
})
export class DocumentationFolderComponent {
  navItem = input<PortalNavigationItem | null>(null);
  pageId = toSignal(
    this.activatedRoute.queryParamMap.pipe(
      map(params => params.get('pageId'))
    ),
    {initialValue: null}
  );
  children = signal<PortalNavigationItem[] | null>(null);
  selectedPageContent = signal<string>('');
  initialized = false;

  constructor(private router: Router,
              private activatedRoute: ActivatedRoute,
              private itemsService: PortalNavigationItemsService) {
    effect(() => this.loadChildren());
    effect(() => this.loadPageContent());
  }

  onSelect(selectedPageId: string | null) {
    if (selectedPageId) {
      this.router.navigate([], {
        relativeTo: this.activatedRoute,
        // replaceUrl: true,
        queryParams: {pageId: selectedPageId}
      });
    }
  }

  private loadChildren() {
    this.children.set(null);

    const navItem = this.navItem();
    const loadFunc$ = navItem ? this.itemsService.getNavigationItems('TOP_NAVBAR', true, navItem.id)
      : of([] as PortalNavigationItem[]);

    loadFunc$.subscribe(data => {
        this.initialized = true;
        this.children.set(data);
      }
    )
  }

  private loadPageContent() {
    const pageId = this.pageId();
    const children = this.children();

    const pageExistsInChildren = children?.find((item) => item.id === pageId);
    const loadFunc$ = pageExistsInChildren ? this.itemsService.getNavigationItemContent(pageId!) : of('');

    loadFunc$.subscribe(content => this.selectedPageContent.set(content));
  }
}





















