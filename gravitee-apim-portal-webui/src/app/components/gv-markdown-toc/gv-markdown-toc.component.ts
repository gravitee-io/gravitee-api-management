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
import { Component, OnInit, OnDestroy, HostListener, ElementRef, AfterViewInit } from '@angular/core';
import { ActivatedRoute, Router } from '@angular/router';
import { lexer, Parser, Renderer, TextRenderer, Tokens } from 'marked';
import { Subscription } from 'rxjs';
import GithubSlugger from 'github-slugger';

import { PageService } from '../../services/page.service';
import { ScrollService } from '../../services/scroll.service';
import { GvDocumentationComponent } from '../gv-documentation/gv-documentation.component';

@Component({
  selector: 'app-gv-markdown-toc',
  templateUrl: './gv-markdown-toc.component.html',
  styleUrls: ['./gv-markdown-toc.component.css'],
  standalone: false,
})
export class GvMarkdownTocComponent implements OnInit, OnDestroy, AfterViewInit {
  tocList: TocModel[];
  currentAnchor: string;
  pageServiceSubscription: Subscription;

  /* for TOC computiung */
  parser = new Parser();
  slugger = new GithubSlugger();
  textRenderer = new TextRenderer();
  /* ****************** */
  private scrollInProgress: boolean;

  constructor(
    private route: ActivatedRoute,
    private pageService: PageService,
    private router: Router,
    private scrollService: ScrollService,
    private element: ElementRef,
  ) {}

  ngOnInit(): void {
    this.route.fragment.subscribe(anchor => {
      if (!this.scrollInProgress) {
        this.currentAnchor = anchor;
      }
    });
    this.pageServiceSubscription = this.pageService.get().subscribe(page => {
      if (page && page.content) {
        this.tocList = this._buildTocModel(page.content);
      }
    });
  }

  ngAfterViewInit() {
    GvDocumentationComponent.reset(this.element.nativeElement);
  }

  @HostListener('window:resize')
  onResize() {
    window.requestAnimationFrame(() => {
      GvDocumentationComponent.updateMenuHeight(this.element.nativeElement);
    });
  }

  @HostListener('window:scroll')
  onScroll() {
    window.requestAnimationFrame(() => {
      GvDocumentationComponent.updateMenuPosition(this.element.nativeElement);
    });
  }

  ngOnDestroy(): void {
    this.pageServiceSubscription.unsubscribe();
  }

  goTo(anchor: string) {
    this.scrollInProgress = true;
    this.currentAnchor = anchor;
    this.scrollService.scrollToAnchor(anchor).then(() => {
      this.scrollInProgress = false;
    });
  }

  _buildTocModel(content: string): TocModel[] {
    const nodeMap = [];

    const tokens = (lexer(content).filter(item => item.type === 'heading' && item.depth > 1) as Tokens.Heading[]).map(item => ({
      anchor: this._computeAnchor(item),
      text: this._computeText(item),
      children: [],
      level: item.depth,
    }));

    for (let index = 0; index < tokens.length; index++) {
      const node: TocModel = tokens[index];
      const parentNode = this._findParentNode(tokens, node, index);
      if (parentNode) {
        parentNode.children.push(node);
      } else {
        nodeMap.push(node);
      }
    }
    return nodeMap;
  }

  _computeText(item: any) {
    return this.parser.parseInline(item.tokens, this.textRenderer as Renderer);
  }

  _computeAnchor(item: any) {
    return this.slugger.slug(this._unescape(this.parser.parseInline(item.tokens, this.textRenderer as Renderer)));
  }

  _findParentNode(tokens: TocModel[], child: TocModel, childIndex: number): any {
    for (let index = childIndex - 1; index > 1; index--) {
      if (tokens[index] && tokens[index].level < child.level) {
        return tokens[index];
      }
    }
  }

  _unescape(html) {
    const unescapeTest = /&(#(?:\d+)|(?:#x[0-9A-Fa-f]+)|(?:\w+));?/gi;

    // explicitly match decimal, hex, and named HTML entities
    return html.replace(unescapeTest, (_, n) => {
      n = n.toLowerCase();
      if (n === 'colon') {
        return ':';
      }
      if (n.charAt(0) === '#') {
        return n.charAt(1) === 'x' ? String.fromCharCode(parseInt(n.substring(2), 16)) : String.fromCharCode(+n.substring(1));
      }
      return '';
    });
  }
}

class TocModel {
  public text: string;
  public children?: TocModel[];
  public level: number;
}
