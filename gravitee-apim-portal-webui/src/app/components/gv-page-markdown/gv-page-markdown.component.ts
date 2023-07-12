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
import { AfterViewInit, Component, ElementRef, HostListener, Input, OnInit, SecurityContext, ViewChild } from '@angular/core';
import { marked } from 'marked';
import hljs from 'highlight.js';
import { Router } from '@angular/router';
import { DomSanitizer } from '@angular/platform-browser';

import { Page } from '../../../../projects/portal-webclient-sdk/src/lib';
import { PageService } from '../../services/page.service';
import { ScrollService } from '../../services/scroll.service';
import { ConfigurationService } from '../../services/configuration.service';

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
  private ANCHOR_CLASSNAME = 'anchor';
  private INTERNAL_LINK_CLASSNAME = 'internal-link';

  constructor(
    private configurationService: ConfigurationService,
    private pageService: PageService,
    private router: Router,
    private scrollService: ScrollService,
    private elementRef: ElementRef,
    private readonly sanitizer: DomSanitizer,
  ) {}

  ngOnInit() {
    this.baseURL = this.configurationService.get('baseURL');

    this.page = this.pageService.getCurrentPage();
    if (this.page && this.page.content) {
      marked.use({ renderer: this.renderer });

      marked.setOptions({
        highlight: (code, language) => {
          const validLanguage = hljs.getLanguage(language) ? language : 'plaintext';
          return hljs.highlight(validLanguage, code).value;
        },
      });

      this.pageContent = this.sanitizer.sanitize(SecurityContext.HTML, marked(this.page.content));
    }
  }

  get renderer() {
    const defaultRenderer = new marked.Renderer();
    // eslint-disable-next-line @typescript-eslint/no-this-alias
    const that = this;
    return {
      image(href, title, text) {
        // is it a portal media ?
        let parsedURL = /\/environments\/(?:[\w-]+)\/portal\/media\/([\w-]+)/g.exec(href);
        if (parsedURL) {
          const portalHref = `${that.baseURL}/media/${parsedURL[1]}`;
          return `<img alt="${text != null ? text : ''}" title="${title != null ? title : ''}" src="${portalHref}" />`;
        } else {
          // is it a API media ?
          parsedURL = /\/environments\/(?:[\w-]+)\/apis\/([\w-]+)\/media\/([\w-]+)/g.exec(href);
          if (parsedURL) {
            const portalHref = `${that.baseURL}/apis/${parsedURL[1]}/media/${parsedURL[2]}`;
            return `<img alt="${text != null ? text : ''}" title="${title != null ? title : ''}" src="${portalHref}" />`;
          }
        }
        return defaultRenderer.image(href, title, text);
      },
      link(href, title, text) {
        // is it a portal page URL ?
        let parsedURL = /\/#!\/settings\/pages\/([\w-]+)/g.exec(href);
        if (!parsedURL) {
          // is it a API page URL ?
          parsedURL = /\/#!\/apis\/(?:[\w-]+)\/documentation\/([\w-]+)/g.exec(href);
        }

        if (parsedURL) {
          const pageId = parsedURL[1];
          return `<a class="${that.INTERNAL_LINK_CLASSNAME}" href="${that.pageBaseUrl}?page=${pageId}">${text}</a>`;
        }

        if (href.startsWith('#')) {
          return `<a class="${that.ANCHOR_CLASSNAME}" href="${href}">${text}</a>`;
        }

        return defaultRenderer.link(href, title, text);
      },
    };
  }

  ngAfterViewInit() {
    this.processOffsets();
    if (this.pageElementsPosition) {
      this.router.navigate([], {
        fragment: this.pageElementsPosition[0] && this.pageElementsPosition[0].id,
        queryParamsHandling: 'preserve',
      });
    }
  }

  processOffsets() {
    const mdContent = this.mdContent && this.mdContent.nativeElement;
    if (mdContent) {
      this.pageElementsPosition = [];
      const markdownElements = Object.values(mdContent.children);
      markdownElements.forEach((element: HTMLElement) => {
        if (element && element.id && ['H2', 'H3', 'H4', 'H5', 'H6'].includes(element.tagName)) {
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
    if (this.pageElementsPosition) {
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
    if ($event.target.tagName.toLowerCase() !== 'a') {
      return true;
    }
    const url = new URL($event.target.href);
    if ($event.target.classList.contains(this.ANCHOR_CLASSNAME)) {
      this.scrollService.scrollToAnchor(url.hash);
      return false;
    } else if ($event.target.classList.contains(this.INTERNAL_LINK_CLASSNAME)) {
      this.router.navigateByUrl(url.pathname + url.search);
      return false;
    }
    return true;
  }
}
