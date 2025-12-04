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
import {Component, effect, Input, input, model, signal} from '@angular/core';
import {MobileClassDirective} from "../../../../directives/mobile-class.directive";
import {MatCard} from "@angular/material/card";
import {SectionNode, TreeComponent} from "./tree-component/tree.component";
import {
  PortalNavigationItem,
  PortalNavigationPage
} from "../../../../entities/portal-navigation/portal-navigation-item";
import {BehaviorSubject, catchError, from, map, Observable, switchMap, tap} from "rxjs";
import {ActivatedRoute, Router} from "@angular/router";
import {PortalNavigationItemsService} from "../../../../services/portal-navigation-items.service";
import {toSignal} from "@angular/core/rxjs-interop";
import {AsyncPipe} from "@angular/common";
import {GraviteeMarkdownViewerModule} from "@gravitee/gravitee-markdown";
import {InnerLinkDirective} from "../../../../directives/inner-link.directive";

@Component({
  selector: 'app-documentation-folder',
  imports: [MobileClassDirective, TreeComponent, AsyncPipe, MatCard, TreeComponent, GraviteeMarkdownViewerModule, InnerLinkDirective],
  standalone: true,
  templateUrl: "./documentation-folder.component.html",
  styleUrl: "./documentation-folder.component.scss",
})
export class DocumentationFolderComponent {
  items = input<PortalNavigationItem[] | null>([]);
  selectedPageContent = model('');

  private readonly pageId$ = this.activatedRoute.queryParams.pipe(map((params) => params['pageId'] ?? null));
  readonly pageId = toSignal(this.pageId$, {initialValue: null});

  constructor(private router: Router,
              private activatedRoute: ActivatedRoute,
              private itemsService: PortalNavigationItemsService) {
    effect(() => {
      if (this.pageId()) {
        this.itemsService.getNavigationItemContent(this.pageId()).subscribe(content => this.selectedPageContent.set(content));
      }
    });
  }

  onSelect(selectedPageId: string) {
    if (selectedPageId) {
      this.router.navigate([], {
        relativeTo: this.activatedRoute,
        queryParams: { pageId: selectedPageId }
      });
    }
  }
}





















