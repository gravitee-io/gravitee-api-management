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
import { Component, computed, effect, input } from '@angular/core';
import { toObservable } from '@angular/core/rxjs-interop';
import { Router } from '@angular/router';
import { catchError, filter, Observable, switchMap } from 'rxjs';
import { of } from 'rxjs/internal/observable/of';

import { DocumentationFolderComponent } from './documentation-folder/documentation-folder.component';
import { NavigationPageFullWidthComponent } from '../../../components/navigation-page-full-width/navigation-page-full-width.component';
import { PortalNavigationItem } from '../../../entities/portal-navigation/portal-navigation-item';
import { PortalPageContent } from '../../../entities/portal-navigation/portal-page-content';
import { PortalNavigationItemsService } from '../../../services/portal-navigation-items.service';

@Component({
  selector: 'app-documentation',
  imports: [DocumentationFolderComponent, AsyncPipe, NavigationPageFullWidthComponent],
  standalone: true,
  template: `
    @if (isItemFolder()) {
      <app-documentation-folder [navItem]="navItem()!" />
    } @else if (isItemPage()) {
      <app-navigation-page-full-width [pageContent]="pageContent$ | async" />
    }
  `,
  styles: `
    :host {
      display: flex;
      flex: 1 1 100%;
    }
  `,
})
export class DocumentationComponent {
  navItem = input.required<PortalNavigationItem>();
  isItemFolder = computed(() => this.navItem()?.type === 'FOLDER');
  isItemPage = computed(() => this.navItem()?.type === 'PAGE');
  pageContent$: Observable<PortalPageContent | null> = toObservable(this.navItem).pipe(
    filter(navItem => navItem?.type === 'PAGE'),
    switchMap(navItem => this.portalNavigationItemsService.getNavigationItemContent(navItem.id)),
    catchError(() => of(null)),
  );
  constructor(
    private readonly router: Router,
    private readonly portalNavigationItemsService: PortalNavigationItemsService,
  ) {
    effect(() => {
      const itemType = this.navItem()?.type;
      if (itemType !== 'FOLDER' && itemType !== 'PAGE') {
        this.router.navigate(['/404']);
      }
    });
  }
}
