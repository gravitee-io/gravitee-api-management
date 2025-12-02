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
import {Component, Input, input} from '@angular/core';
import {MobileClassDirective} from "../../../../directives/mobile-class.directive";
import {MatCard} from "@angular/material/card";
import {SectionNode, TreeComponent} from "./tree-component/tree.component";
import {
  PortalNavigationItem,
  PortalNavigationPage
} from "../../../../entities/portal-navigation/portal-navigation-item";
import {BehaviorSubject, catchError, map, Observable, switchMap, tap} from "rxjs";
import {of} from "rxjs/internal/observable/of";
import {shareReplay} from "rxjs/operators";
import {ActivatedRoute, Router} from "@angular/router";
import {PortalNavigationItemsService} from "../../../../services/portal-navigation-items.service";
import {PortalPageContentService} from "../../../../services/portal-page-content.service";
import {toSignal} from "@angular/core/rxjs-interop";
import {AsyncPipe} from "@angular/common";

@Component({
  selector: 'app-documentation-folder',
  imports: [MobileClassDirective, TreeComponent, AsyncPipe, MatCard, TreeComponent],
  standalone: true,
  templateUrl: "./documentation-folder.component.html",
  styleUrl: "./documentation-folder.component.scss",
})
export class DocumentationFolderComponent {
  items = input<PortalNavigationItem[] | null>([]);

  private readonly navId$ = this.activatedRoute.queryParams.pipe(map((params) => params['navId'] ?? null));
  readonly navId = toSignal(this.navId$, { initialValue: null });

  // readonly menuLinks$: Observable<PortalNavigationItem[]> = this.items.pipe(
  //   switchMap(() => this.portalNavigationItemsService.getNavigationItems('TOP_NAVBAR')),
  //   map((response) => response ?? []),
  //   tap((items) => {
  //     const currentNavId = this.navId();
  //
  //     // If no navId in query params, navigate to first PAGE item
  //     if (items && items.length > 0 && !currentNavId) {
  //       const firstPage = items.find((i) => i.type === 'PAGE');
  //       if (firstPage) {
  //         this.navigateToItemByNavId(firstPage.id);
  //       }
  //     }
  //   }),
  //   catchError(() => {
  //     console.log('my custom error');
  //     // this.snackBarService.error('Failed to load navigation items');
  //     return of([]);
  //   }),
  //   shareReplay({ bufferSize: 1, refCount: true }),
  // );

  constructor(private router: Router,
              private activatedRoute: ActivatedRoute,
              private portalNavigationItemsService: PortalNavigationItemsService,
              private contentService: PortalPageContentService) {
    activatedRoute.queryParams.subscribe(res => {
      const selectedNavId = res['data']['selectedNavId'];
      console.log('selectedNavId', selectedNavId);
    //   const seletedItem = res.find(item => item.id === selectedNavId) as PortalNavigationPage;
    //   if (seletedItem) {
    //     contentService.getPageContent(seletedItem.portalPageContentId).subscribe(res => console.log('res', res))
    //   }
    })
  }

  onSelect(selectedItem: SectionNode) {
    // this.router
    //   .navigate(['.'], {
    //     relativeTo: this.activatedRoute,
    //     queryParams: { navId },
    //     queryParamsHandling: 'merge',
    //   })
    this.router.navigate([], {
      relativeTo: this.activatedRoute,
      queryParams: { selectedNavId: selectedItem.id }
    });

      const seletedItem = this.items()?.find(item => item.id === selectedItem.id) as PortalNavigationPage;
      console.log('seletedItem', seletedItem);
      if (seletedItem) {
        this.contentService.getPageContent(seletedItem.portalPageContentId).subscribe(res => console.log('res', res))
      }
  }

  private navigateToItemByNavId(navId: string): void {
    this.router
      .navigate(['.'], {
        relativeTo: this.activatedRoute,
        queryParams: { navId },
        queryParamsHandling: 'merge',
      })
      // .catch(() => this.snackBarService.error('Failed to navigate to portal navigation item: ' + navId));
  }
}





















