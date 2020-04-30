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
import { Component, OnInit, Input, HostListener, AfterViewChecked, AfterViewInit, ViewChild, ElementRef } from '@angular/core';
import * as marked from 'marked';
import { PageService } from 'src/app/services/page.service';
import { ActivatedRoute, Router } from '@angular/router';
import { ScrollService } from 'src/app/services/scroll.service';
import { Location } from '@angular/common';

@Component({
  selector: 'app-gv-page-markdown',
  templateUrl: './gv-page-markdown.component.html',
  styleUrls: ['./gv-page-markdown.component.css'],
})

export class GvPageMarkdownComponent implements OnInit, AfterViewInit {

  @Input() withToc: boolean;

  pageContent: string;
  pageElementsPosition: any[];

  @ViewChild('mdContent', { static: false }) mdContent: ElementRef;

  constructor(
    private pageService: PageService,
    private router: Router,
    private route: ActivatedRoute,
    private scrollService: ScrollService,
  ) { }

  ngOnInit() {
    const page = this.pageService.getCurrentPage();
    if (page && page.content) {
      this.pageContent = marked(page.content);
    }
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
            offsetTop: document.getElementById(element.id).offsetTop - this.scrollService.getHeaderHeight()
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
        const nextItem = this.pageElementsPosition[index+1];
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
}
