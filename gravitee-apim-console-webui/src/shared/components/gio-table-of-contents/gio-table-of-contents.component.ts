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
import { DOCUMENT, Location } from '@angular/common';
import { AfterViewInit, ChangeDetectorRef, Component, ElementRef, Inject, Input, OnDestroy, OnInit } from '@angular/core';
import { flatten, sortBy } from 'lodash';
import { fromEvent, Observable, Subscription } from 'rxjs';
import { debounceTime, map, shareReplay, tap } from 'rxjs/operators';

import { TocSection, TocSectionLink } from './TocSection';
import { GioTableOfContentsService } from './gio-table-of-contents.service';

@Component({
  selector: 'gio-table-of-contents',
  template: require('./gio-table-of-contents.component.html'),
  styles: [require('./gio-table-of-contents.component.scss')],
})
export class GioTableOfContentsComponent implements OnInit, AfterViewInit, OnDestroy {
  @Input()
  public scrollingContainer: string | HTMLElement;

  @Input()
  public set sectionNames(sectionNames: Record<string, string>) {
    Object.entries(sectionNames).forEach(([sectionId, sectionName]) => {
      this.tableOfContentsService.addSection(sectionId, sectionName);
    });
  }

  sections$: Observable<TocSection[]>;

  rootUrl: string;

  private allLinks: TocSectionLink[] = [];

  private subscriptions = new Subscription();

  private container: HTMLElement | Window;

  private fragment?: string;

  constructor(
    private readonly tableOfContentsService: GioTableOfContentsService,
    @Inject(DOCUMENT) private readonly document: Document,
    private readonly elementRef: ElementRef,
    private readonly location: Location,
    private changeDetectorRef: ChangeDetectorRef,
  ) {}

  ngOnInit(): void {
    this.container =
      this.scrollingContainer instanceof HTMLElement
        ? this.scrollingContainer
        : (this.document.querySelector(this.scrollingContainer) as HTMLElement) || window;

    /**
     * TODO: remove me after angularJs migration
     * 🤷‍♂️ Without knowing the exact reason :
     * Added to fix a problem related to the navigation between AngularJs and Angular.
     * Because `this.location.path` was always returning the url of the previous route
     * and not the current one. With a setTimeout it seems to fix the problem. I suspect an
     * import or execution order problem but i didn't find better
     */
    setTimeout(() => {
      // Set initial route url/location and fragment if defined
      const { rootUrl, fragment } = splitUrlFragment(this.location.path(true));
      this.rootUrl = rootUrl;
      this.fragment = fragment;
    }, 1);

    this.subscriptions.add(
      // Update rootUrl, fragment and scroll position if url/location change
      this.location.subscribe((event) => {
        const { rootUrl, fragment } = splitUrlFragment(event.url);

        if (rootUrl !== this.rootUrl) {
          this.rootUrl = rootUrl;
        }

        if (fragment !== this.fragment) {
          this.fragment = fragment;

          this.updateScrollPosition();
        }
      }),
    );

    this.subscriptions.add(
      fromEvent(this.container, 'scroll')
        .pipe(debounceTime(10))
        .subscribe(() => this.onScroll()),
    );

    this.sections$ = this.tableOfContentsService.getSections$().pipe(
      map((s) => Object.values(s)),
      tap((s) =>
        // Get all links for the scroll activation mechanism
        {
          this.allLinks = this.sortByTopOffset(flatten(s.map((s) => s.links)));
          this.changeDetectorRef.detectChanges();
        },
      ),
      shareReplay(),
    );
  }

  ngAfterViewInit() {
    // FIXME : try better way than setTimeout
    // Wait 300ms before trying to update the scroll position when the user arrives on the page
    // I guess as the links are in the parent component of TableOfContentsComponent the AfterViewInit comes too early 🤷
    setTimeout(() => {
      this.updateScrollPosition();
    }, 300);
  }

  ngOnDestroy() {
    this.subscriptions.unsubscribe();
  }

  onClick(event: PointerEvent, linkId: string) {
    // Use location go emit for location.subscribe to update scroll position
    event.stopPropagation();
    this.location.go(`${this.rootUrl}#${linkId}`);
  }

  sortByTopOffset(links: TocSectionLink[] = []) {
    return sortBy(links, 'top');
  }

  private updateScrollPosition(): void {
    this.document.getElementById(`toc-${this.fragment}`)?.scrollIntoView();
  }

  private getTableOfContentsTop(): number {
    const { top } = this.elementRef.nativeElement.getBoundingClientRect();
    return top;
  }

  // Change current active link according to the scroll
  private onScroll(): void {
    // 🧙‍♂️ Find the scroll offset with the TOC top position and add an extra 32 offset to activate the link a little before
    const scrollOffset = this.getTableOfContentsTop() + 32;

    let hasChanged = false;

    for (let i = 0; i < this.allLinks.length; i++) {
      // A link is considered active if the page is scrolled past the
      // anchor without also being scrolled passed the next link.
      const currentLink = this.allLinks[i];
      const nextLink = this.allLinks[i + 1];
      // 📝 link.top is a getter and always return a relative link position from the viewport
      const isActive = currentLink.top <= 0 + scrollOffset && (!nextLink || nextLink.top > 0 + scrollOffset);

      if (isActive !== currentLink.active) {
        currentLink.active = isActive;

        hasChanged = true;
      }
    }

    if (hasChanged) {
      this.tableOfContentsService.markAsChanged();
    }
  }
}

// Split url to remove last # followed by kebabCase string
const splitUrlFragment = (url: string): { rootUrl: string; fragment?: string } => {
  const [rootUrl, fragment] = url.split(/#(([a-z0-9]*)(-[a-z0-9]+)*)$/);
  return { rootUrl, fragment };
};
