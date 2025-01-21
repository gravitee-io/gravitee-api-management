/*
 * Copyright (C) 2024 The Gravitee team (http://gravitee.io)
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
import { Component, Input, OnChanges, ViewEncapsulation } from '@angular/core';
import { DomSanitizer, SafeHtml } from '@angular/platform-browser';
import Processor from 'asciidoctor';

@Component({
  selector: 'app-page-asciidoc',
  imports: [],
  styleUrl: './page-asciidoc.component.scss',
  encapsulation: ViewEncapsulation.ShadowDom,
  template: ` <div id="asciidoc" [innerHTML]="asciidoc"></div>`,
})
export class PageAsciidocComponent implements OnChanges {
  @Input() content!: string | undefined;
  asciidoc!: SafeHtml;

  constructor(private domSanitizer: DomSanitizer) {}

  ngOnChanges(): void {
    const processor = Processor();
    const content = processor.convert(this.content ?? '', { attributes: { showtitle: true } }) as string;
    const parser = new DOMParser();
    const document = parser.parseFromString(content, 'text/html');
    this.asciidoc = this.domSanitizer.bypassSecurityTrustHtml(document.body.outerHTML);
  }
}
