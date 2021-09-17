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
import { AfterViewInit, Component, ElementRef, Inject, Input, OnDestroy, OnInit } from '@angular/core';
import { flatten } from 'lodash';
import { fromEvent, Observable, Subscription } from 'rxjs';
import { debounceTime, map } from 'rxjs/operators';

import { TocSection, TocSectionLink } from './TocSection';
import { TableOfContentsService } from './table-of-contents.service';

@Component({
  selector: 'table-of-contents',
  template: require('./table-of-contents.component.html'),
  styles: [require('./table-of-contents.component.scss')],
})
export class TableOfContentsComponent implements OnInit, AfterViewInit, OnDestroy {
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
    private readonly tableOfContentsService: TableOfContentsService,
    @Inject(DOCUMENT) private readonly document: Document,
    private readonly elementRef: ElementRef,
    private readonly location: Location,
  ) {}

  ngOnInit(): void {
    this.container =
      this.scrollingContainer instanceof HTMLElement
        ? this.scrollingContainer
        : (this.document.querySelector(this.scrollingContainer) as HTMLElement) || window;

    // Set initial route url/location and fragment if defined
    const { rootUrl, fragment } = splitUrlFragment(this.location.path(true));
    this.rootUrl = rootUrl;
    this.fragment = fragment;

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

    this.sections$ = this.tableOfContentsService.getSections$().pipe(map((s) => Object.values(s)));

    // Get all links for the scroll activation mechanism
    this.subscriptions.add(
      this.sections$.subscribe((s) => {
        this.allLinks = flatten(s.map((s) => s.links));
      }),
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

  private updateScrollPosition(): void {
    this.document.getElementById(`toc-${this.fragment}`)?.scrollIntoView();
  }

  // Gets the scroll offset of the scroll container
  private getScrollOffset(): number | void {
    const { top } = this.elementRef.nativeElement.getBoundingClientRect();

    const container = this.container;

    if (container instanceof HTMLElement) {
      return container.scrollTop + top;
    }

    if (container) {
      return container.pageYOffset + top;
    }
  }

  // Change current active link according to the scroll
  private onScroll(): void {
    const scrollOffset = this.getScrollOffset();
    let hasChanged = false;

    for (let i = 0; i < this.allLinks.length; i++) {
      // A link is considered active if the page is scrolled past the
      // anchor without also being scrolled passed the next link.
      const currentLink = this.allLinks[i];
      const nextLink = this.allLinks[i + 1];
      const isActive = scrollOffset >= currentLink.top && (!nextLink || nextLink.top >= scrollOffset);

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
  const [rootUrl, fragment] = url.split(/#([a-z0-9]*)(-[a-z0-9]+)*$/);
  return { rootUrl, fragment };
};
