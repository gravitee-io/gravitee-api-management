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
import { AfterViewInit, Component, ElementRef, HostListener, Input, OnInit, ViewChild } from '@angular/core';
import { ActivatedRoute, Router } from '@angular/router';
import { DomSanitizer } from '@angular/platform-browser';

import { Page } from '../../../../projects/portal-webclient-sdk/src/lib';
import { PageService } from '../../services/page.service';
import { ScrollService } from '../../services/scroll.service';
import { ConfigurationService } from '../../services/configuration.service';
import { MarkdownService } from '../../services/markdown.service';

@Component({
  selector: 'app-gv-page-markdown',
  templateUrl: './gv-page-markdown.component.html',
  styleUrls: ['./gv-page-markdown.component.css'],
})
export class GvPageMarkdownComponent implements OnInit, AfterViewInit {
  @Input() withToc: boolean;
  @Input() pageBaseUrl: string;

  pageContent: string;
  pageElementsPosition: any[];
  page: Page;
  baseURL: string;

  @ViewChild('mdContent', { static: false }) mdContent: ElementRef;

  constructor(
    private configurationService: ConfigurationService,
    private pageService: PageService,
    private router: Router,
    private activatedRoute: ActivatedRoute,
    private scrollService: ScrollService,
    private markdownService: MarkdownService,
    private elementRef: ElementRef,
    private readonly sanitizer: DomSanitizer,
  ) {}

  ngOnInit() {
    this.baseURL = this.configurationService.get('baseURL');

    this.page = this.pageService.getCurrentPage();
    if (this.page?.content) {
      this.pageContent = this.markdownService.render(this.page.content, this.baseURL, this.pageBaseUrl);
    }
  }

  ngAfterViewInit() {
    this.processOffsets();

    // Best effort to scroll to the anchor after markdown is rendered
    setTimeout(() => {
      const fragment = this.activatedRoute.snapshot.fragment;
      if (fragment && this.pageElementsPosition?.map(e => e.id).includes(fragment)) {
        this.scrollService.scrollToAnchor(fragment);
      }
    }, 1000);
  }

  processOffsets() {
    const mdContent = this.mdContent && this.mdContent.nativeElement;
    if (mdContent) {
      this.pageElementsPosition = [];
      const markdownElements = Object.values(mdContent.children);
      markdownElements.forEach((element: HTMLElement) => {
        if (element && element.id && ['h2', 'h3', 'h4', 'h5', 'h6'].includes(element.tagName.toLowerCase())) {
          this.pageElementsPosition.push({
            id: element.id,
            offsetTop: document.getElementById(element.id).offsetTop - ScrollService.getHeaderHeight(),
          });
        }
      });
    }
  }

  @HostListener('window:scroll')
  onScroll() {
    this.processOffsets();
    if (this.pageElementsPosition && this.pageElementsPosition.length > 0) {
      let anchor: string;
      const currentYPosition = window.pageYOffset;
      for (let index = 0; index < this.pageElementsPosition.length && !anchor; index++) {
        const item = this.pageElementsPosition[index];
        const nextItem = this.pageElementsPosition[index + 1];
        if (currentYPosition < item.offsetTop) {
          anchor = null;
        } else if (currentYPosition >= item.offsetTop && (!nextItem || (nextItem && currentYPosition < nextItem.offsetTop))) {
          anchor = item.id;
        }
      }
      this.router.navigate([], {
        fragment: anchor || (this.pageElementsPosition[0] && this.pageElementsPosition[0].id),
        queryParamsHandling: 'preserve',
      });
    }
  }

  openMedia(link: string) {
    window.open(link, '_blank');
  }

  @HostListener('click', ['$event'])
  onClick($event) {
    if ($event.target.tagName.toLowerCase() !== 'a' || !$event.target.href) {
      return true;
    }
    const url = new URL($event.target.href);
    if ($event.target.classList.contains(this.markdownService.getAnchorClassName())) {
      this.scrollService.scrollToAnchor(url.hash);
      return false;
    } else if ($event.target.classList.contains(this.markdownService.getInternalClassName())) {
      this.router.navigateByUrl(url.pathname + url.search);
      return false;
    }
    return true;
  }
}
