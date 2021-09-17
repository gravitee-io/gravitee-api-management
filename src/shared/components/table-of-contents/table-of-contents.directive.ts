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

import { Directive, ElementRef, Input, OnDestroy, OnInit } from '@angular/core';
import { kebabCase } from 'lodash';

import { TableOfContentsService } from './table-of-contents.service';

@Directive({
  selector: 'h2[tableOfContents], h3[tableOfContents], h4[tableOfContents]',
})
export class TableOfContentsDirective implements OnInit, OnDestroy {
  constructor(private readonly el: ElementRef, private readonly tableOfContentsService: TableOfContentsService) {}

  @Input('tableOfContentsSectionId') sectionId = '';

  ngOnInit(): void {
    const name = this.el.nativeElement.innerText?.trim();
    const type = String(this.el.nativeElement.tagName).toLowerCase();
    const id = kebabCase(name);
    const { top } = this.el.nativeElement.getBoundingClientRect();

    this.el.nativeElement.id = `toc-${id}`;

    this.tableOfContentsService.addLink(this.sectionId, { active: false, id, name, top, type });
  }

  ngOnDestroy(): void {
    const name = this.el.nativeElement.innerText;
    this.tableOfContentsService.removeLink(this.sectionId, kebabCase(name));
  }
}
