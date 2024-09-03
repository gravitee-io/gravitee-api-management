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
import { Component, Input } from '@angular/core';

import { PageAsciidocComponent } from './page-asciidoc/page-asciidoc.component';
import { PageAsyncApiComponent } from './page-async-api/page-async-api.component';
import { PageMarkdownComponent } from './page-markdown/page-markdown.component';
import { PageRedocComponent } from './page-redoc/page-redoc.component';
import { PageSwaggerComponent } from './page-swagger/page-swagger.component';
import { Page } from '../../entities/page/page';

@Component({
  selector: 'app-page',
  standalone: true,
  imports: [PageSwaggerComponent, PageMarkdownComponent, PageAsciidocComponent, PageAsyncApiComponent, PageRedocComponent],
  templateUrl: './page.component.html',
  styleUrl: './page.component.scss',
})
export class PageComponent {
  @Input()
  page!: Page;
  @Input()
  pages: Page[] = []; // Used to create links in Markdown to other pages within the scope
  @Input()
  apiId?: string;
}
