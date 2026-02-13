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
import { AfterViewInit, ChangeDetectorRef, Component, ElementRef, ViewChild } from '@angular/core';
import { ActivatedRoute, Router, RoutesRecognized } from '@angular/router';
import { Subject } from 'rxjs';
import { distinctUntilChanged, filter, map, startWith, takeUntil } from 'rxjs/operators';

import { ConsoleExtensionRegistryService } from '../services-ngx/console-extension-registry.service';
import { ConsoleExtensionPlacement } from '../services-ngx/console-extension-loader';

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
      <div class="content">
        @if (hasTopPlugins) {
          <div class="content-top" #topSlot></div>
        }
        <div class="content-middle">
          @if (hasLeftPlugins) {
            <div class="content-left" #leftSlot></div>
          }
          <div id="gio-toc-scrolling-container" class="content-center">
            @if (!isLoading) {
              <router-outlet></router-outlet>
            }
          </div>
          @if (hasRightPlugins) {
            <div class="content-right" #rightSlot></div>
          }
        </div>
        @if (hasBottomPlugins) {
          <div class="content-bottom" #bottomSlot></div>
        }
        <div class="content-overlay" #overlaySlot></div>
      </div>
      @if (openContextualDoc) {
        <gio-contextual-doc
          class="documentation"
          [contextualDocumentationPage]="contextualDocumentationPage"
          (onClose)="openContextualDocumentationPage(false)"
        ></gio-contextual-doc>
      }
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
        overflow-y: scroll;
        scrollbar-width: none;
      }

      .content {
        grid-area: content;
        display: flex;
        flex-direction: column;
        overflow: hidden;
        position: relative;
      }

      .content-top,
      .content-bottom {
        flex-shrink: 0;
      }

      .content-middle {
        display: flex;
        flex: 1;
        overflow: hidden;
      }

      .content-left,
      .content-right {
        flex-shrink: 0;
        overflow: auto;
      }

      .content-center {
        flex: 1;
        overflow: auto;
      }

      .content-overlay {
        position: absolute;
        inset: 0;
        pointer-events: none;
        z-index: 10;
      }

      .content-overlay > * {
        pointer-events: auto;
      }

      .documentation {
        grid-area: documentation;
      }
    `,
  ],
  standalone: false,
})
export class ManagementComponent implements AfterViewInit {
  private unsubscribe$ = new Subject<void>();
  private contextualDocVisibilityKey = 'gv-contextual-doc-visibility';

  @ViewChild('overlaySlot') overlaySlot: ElementRef;
  @ViewChild('topSlot') topSlot: ElementRef;
  @ViewChild('bottomSlot') bottomSlot: ElementRef;
  @ViewChild('leftSlot') leftSlot: ElementRef;
  @ViewChild('rightSlot') rightSlot: ElementRef;

  openContextualDoc: boolean = localStorage.getItem(this.contextualDocVisibilityKey) === 'true';
  contextualDocumentationPage: string;
  public isLoading = false;

  public hasTopPlugins = false;
  public hasBottomPlugins = false;
  public hasLeftPlugins = false;
  public hasRightPlugins = false;

  constructor(
    public readonly router: Router,
    public readonly activatedRoute: ActivatedRoute,
    private changeDetectorRef: ChangeDetectorRef,
    private consoleExtensionRegistryService: ConsoleExtensionRegistryService,
  ) {
    this.hasTopPlugins = this.consoleExtensionRegistryService.getComponentsByPlacement('top').length > 0;
    this.hasBottomPlugins = this.consoleExtensionRegistryService.getComponentsByPlacement('bottom').length > 0;
    this.hasLeftPlugins = this.consoleExtensionRegistryService.getComponentsByPlacement('left').length > 0;
    this.hasRightPlugins = this.consoleExtensionRegistryService.getComponentsByPlacement('right').length > 0;
  }

  ngOnInit() {
    // Necessary to refresh view when envId changes
    this.activatedRoute.params
      .pipe(
        map((p) => p.envHrid),
        distinctUntilChanged(),
        takeUntil(this.unsubscribe$),
      )
      .subscribe({
        next: (_) => {
          this.isLoading = true;
          try {
            this.changeDetectorRef.detectChanges();
          } finally {
            this.isLoading = false;
          }
        },
      });

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

  ngAfterViewInit() {
    this.mountPlugins('overlay', this.overlaySlot);
    this.mountPlugins('top', this.topSlot);
    this.mountPlugins('bottom', this.bottomSlot);
    this.mountPlugins('left', this.leftSlot);
    this.mountPlugins('right', this.rightSlot);
  }

  private mountPlugins(placement: ConsoleExtensionPlacement, slot: ElementRef | undefined) {
    if (!slot) {
      return;
    }
    const plugins = this.consoleExtensionRegistryService.getComponentsByPlacement(placement);
    for (const plugin of plugins) {
      const el = document.createElement(plugin.tagName);
      el.setAttribute('placement', placement);
      slot.nativeElement.appendChild(el);
    }
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
