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
import { Component } from '@angular/core';
import { ActivatedRoute, Router, RoutesRecognized } from '@angular/router';
import { Subject } from 'rxjs';
import { filter, map, startWith, takeUntil } from 'rxjs/operators';

@Component({
  selector: 'management-root',
  template: `
    <div class="wrapper" [class.withDocumentation]="openContextualDoc">
      <gio-top-nav
        [displayContextualDocumentationButton]="!!contextualDocumentationPage"
        (openContextualDocumentationPage)="openContextualDocumentationPage(true)"
        class="header"
      ></gio-top-nav>
      <gio-side-nav class="sidebar"></gio-side-nav>
      <div class="content gio-toc-scrolling-container"><router-outlet></router-outlet></div>
      <gio-contextual-doc
        class="documentation"
        *ngIf="openContextualDoc"
        [contextualDocumentationPage]="contextualDocumentationPage"
        (onClose)="openContextualDocumentationPage(false)"
      ></gio-contextual-doc>
    </div>
  `,

  styles: [
    `
      .wrapper {
        display: grid;
        grid-template-rows: 70px calc(100vh - 70px);
        grid-template-columns: min-content auto;
        grid-template-areas:
          'header  header  '
          'sidebar content ';
        height: 100vh;

        &.withDocumentation {
          grid-template-rows: 70px calc(100vh - 70px) calc(100vh - 70px);
          grid-template-columns: min-content auto min-content;
          grid-template-areas:
            'header  header  header'
            'sidebar content documentation';
        }
      }

      .header {
        grid-area: header;
      }

      .sidebar {
        grid-area: sidebar;
      }

      .content {
        grid-area: content;
        overflow: auto;
      }

      .documentation {
        grid-area: documentation;
      }
    `,
  ],
})
export class ManagementComponent {
  private unsubscribe$ = new Subject<void>();
  private contextualDocVisibilityKey = 'gv-contextual-doc-visibility';

  openContextualDoc: boolean = localStorage.getItem(this.contextualDocVisibilityKey) === 'true';
  contextualDocumentationPage: string;

  constructor(public readonly router: Router, public readonly activatedRoute: ActivatedRoute) {}

  ngOnInit() {
    this.router.events
      .pipe(
        filter((event) => event instanceof RoutesRecognized),
        map((event: RoutesRecognized) => event.state.root),
        startWith(this.activatedRoute.snapshot),
        map((route) => {
          while (route.firstChild) {
            route = route.firstChild;
          }

          return route;
        }),
        takeUntil(this.unsubscribe$),
      )
      .subscribe((route) => {
        this.contextualDocumentationPage = route.data.docs?.page ?? undefined;
      });
  }

  ngOnDestroy() {
    this.unsubscribe$.next();
    this.unsubscribe$.unsubscribe();
  }

  openContextualDocumentationPage(open: boolean) {
    localStorage.setItem(this.contextualDocVisibilityKey, String(open));
    this.openContextualDoc = open;
  }
}
