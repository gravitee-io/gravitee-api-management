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
import { AfterViewInit, Component, ElementRef, Input } from '@angular/core';

import { Page } from '../../../entities/page/page';
import { RedocService } from '../../../services/redoc.service';

@Component({
  selector: 'app-page-redoc',
  standalone: true,
  imports: [],
  template: `
    <style>
      .api-tab-documentation__side-bar {
        width: 10%;
      }
    </style>
    <div id="redoc"></div>
  `,
})
export class PageRedocComponent implements AfterViewInit {
  @Input()
  page!: Page;

  constructor(
    private element: ElementRef,
    private redocService: RedocService,
  ) {}

  ngAfterViewInit() {
    const redocElement = this.element.nativeElement.querySelector('#redoc');

    // Force the right-side panel to join into the middle panel
    const options = { theme: { breakpoints: { medium: '150rem' } } };

    this.redocService.init(this.page.content, options, redocElement);
  }
}
