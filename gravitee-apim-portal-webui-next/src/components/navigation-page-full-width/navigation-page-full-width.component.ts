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
import { Component, inject, input, signal } from '@angular/core';

import { NavigationItemContentViewerComponent } from '../navigation-item-content-viewer/navigation-item-content-viewer.component';
import { DocumentationSkeletonComponent } from '../documentation-skeleton/documentation-skeleton.component';
import { PortalNavigationItemsService } from 'src/services/portal-navigation-items.service';
import { PortalNavigationItem } from 'src/entities/portal-navigation/portal-navigation-item';
import { toObservable, toSignal } from '@angular/core/rxjs-interop';
import { catchError, filter, finalize, of } from 'rxjs';
import { switchMap } from 'rxjs';
import { PortalPageContent } from 'src/entities/portal-navigation/portal-page-content';

@Component({
  selector: 'app-navigation-page-full-width',
  imports: [NavigationItemContentViewerComponent, DocumentationSkeletonComponent],
  template: `
    <div class="content">
      <!-- @if (isLoadingPageContent()) { -->
       <div class="navigation-page-full-width__skeleton-container">
        <app-documentation-skeleton
        animate.leave="navigation-page-full-width__skeleton--leave"
        class="navigation-page-full-width__skeleton"
        aria-busy="true"
        i18n-aria-label="@@navigationPageFullWidthContentLoadingAria"
        aria-label="Loading navigation page full width content"
        />
      </div>
      <!-- } @else if (pageContent()) {
        <app-navigation-item-content-viewer class="navigation-page-full-width__content" animate.enter="navigation-page-full-width__content-enter" [pageContent]="pageContent()" />
      } -->
    </div>
  `,
  styleUrl: './navigation-page-full-width.component.scss',
})
export class NavigationPageFullWidthComponent {
  private readonly portalNavigationItemsService = inject(PortalNavigationItemsService);
  navItem = input.required<PortalNavigationItem>();
  isLoadingPageContent = signal(false);
  pageContent = toSignal(toObservable(this.navItem).pipe(
    filter(navItem => navItem?.type === 'PAGE'),
    switchMap(navItem => {
      this.isLoadingPageContent.set(true);
      return this.portalNavigationItemsService.getNavigationItemContent(navItem.id).pipe(
        finalize(() => this.isLoadingPageContent.set(false)),
        catchError(() => of<PortalPageContent>({ type: 'GRAVITEE_MARKDOWN', content: '' })),
      );
    }),
    catchError(() => of<PortalPageContent>({ type: 'GRAVITEE_MARKDOWN', content: '' })),
  ), { initialValue: null });
}
