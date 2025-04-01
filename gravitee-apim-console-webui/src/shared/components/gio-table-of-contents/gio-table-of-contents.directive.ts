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

import { AfterViewInit, Directive, ElementRef, Input, OnDestroy } from '@angular/core';
import { kebabCase } from 'lodash';

import { GioTableOfContentsService } from './gio-table-of-contents.service';
import { TocSectionLink } from './TocSection';

@Directive({
  selector: 'h2[gioTableOfContents], h3[gioTableOfContents], h4[gioTableOfContents], [mat-subheader][gioTableOfContents]',
  standalone: false,
})
export class GioTableOfContentsDirective implements AfterViewInit, OnDestroy {
  constructor(
    private readonly el: ElementRef,
    private readonly tableOfContentsService: GioTableOfContentsService,
  ) {}

  @Input('gioTableOfContentsSectionId') sectionId = '';

  @Input('gioTableOfContentsName') name?: string;

  private link: TocSectionLink;

  ngAfterViewInit(): void {
    this.link = new TocSectionLink(this.el.nativeElement, { name: this.name });

    this.el.nativeElement.id = `toc-${this.link.id}`;

    this.tableOfContentsService.addLink(this.sectionId, this.link);
  }

  ngOnDestroy(): void {
    this.tableOfContentsService.removeLink(this.sectionId, kebabCase(this.link.id));
  }
}
