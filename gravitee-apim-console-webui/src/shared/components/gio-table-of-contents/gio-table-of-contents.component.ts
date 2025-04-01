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
import { DOCUMENT } from '@angular/common';
import { AfterViewInit, ChangeDetectorRef, Component, ElementRef, Inject, Input, OnDestroy, OnInit } from '@angular/core';
import { flatten, sortBy } from 'lodash';
import { fromEvent, Observable, Subject } from 'rxjs';
import { debounceTime, filter, map, shareReplay, takeUntil, tap } from 'rxjs/operators';
import { ActivatedRoute } from '@angular/router';

import { TocSection, TocSectionLink } from './TocSection';
import { GioTableOfContentsService } from './gio-table-of-contents.service';

@Component({
  selector: 'gio-table-of-contents',
  templateUrl: './gio-table-of-contents.component.html',
  styleUrls: ['./gio-table-of-contents.component.scss'],
  standalone: false,
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

  private container: HTMLElement | Window;

  private unsubscribe$ = new Subject<void>();

  constructor(
    private readonly tableOfContentsService: GioTableOfContentsService,
    @Inject(DOCUMENT) private readonly document: Document,
    private readonly elementRef: ElementRef,
    private readonly activatedRoute: ActivatedRoute,
    private changeDetectorRef: ChangeDetectorRef,
  ) {}

  ngOnInit(): void {
    this.container =
      this.scrollingContainer instanceof HTMLElement
        ? this.scrollingContainer
        : (this.document.querySelector(this.scrollingContainer) as HTMLElement) || window;

    fromEvent(this.container, 'scroll')
      .pipe(debounceTime(10), takeUntil(this.unsubscribe$))
      .subscribe(() => this.onScroll());

    this.sections$ = this.tableOfContentsService.getSections$().pipe(
      map((s) => Object.values(s)),
      tap((s) =>
        // Get all links for the scroll activation mechanism
        {
          this.allLinks = this.sortByTopOffset(flatten(s.map((s) => s.links)));
          this.changeDetectorRef.detectChanges();
        },
      ),
      shareReplay(1),
      takeUntil(this.unsubscribe$),
    );
  }

  ngAfterViewInit() {
    this.activatedRoute.fragment
      .pipe(
        debounceTime(300),
        filter((f) => !!f),
        takeUntil(this.unsubscribe$),
      )
      .subscribe((f) => {
        const element = document.querySelector('#toc-' + f);
        if (element) {
          element.scrollIntoView();
        }
      });
  }

  ngOnDestroy() {
    this.unsubscribe$.unsubscribe();
  }

  sortByTopOffset(links: TocSectionLink[] = []) {
    return sortBy(links, 'top');
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
