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
import { Component, inject, Input } from '@angular/core';

import { PageAsciidocComponent } from './page-asciidoc/page-asciidoc.component';
import { PageAsyncApiComponent } from './page-async-api/page-async-api.component';
import { PageMarkdownComponent } from './page-markdown/page-markdown.component';
import { PageGraviteeComponent } from './page-gravitee/page-gravitee.component';
import { PageRedocComponent } from './page-redoc/page-redoc.component';
import { PageSwaggerComponent } from './page-swagger/page-swagger.component';
import { Page } from '../../entities/page/page';
import { ViewerEnum } from '../../entities/page/page-configuration';
import { ConfigService } from '../../services/config.service';

@Component({
  selector: 'app-page',
  imports: [
    PageSwaggerComponent,
    PageMarkdownComponent,
    PageAsciidocComponent,
    PageAsyncApiComponent,
    PageRedocComponent,
    PageGraviteeComponent,
  ],
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

  private configService = inject(ConfigService);

  protected readonly viewerEnum = ViewerEnum;

  get effectiveViewer(): ViewerEnum {
    // Match portal-gamma runtime behavior: portal-level defaultType wins over the page's
    // stored viewer (page viewer remains an authoring/editor preference).
    const configuredViewer =
      this.configService.configuration.openAPIDocViewer?.openAPIDocType?.defaultType ?? this.page.configuration?.viewer;
    const normalized = configuredViewer?.toLowerCase();

    if (normalized === ViewerEnum.Gravitee.toLowerCase()) {
      return ViewerEnum.Gravitee;
    }
    if (normalized === ViewerEnum.Redoc.toLowerCase()) {
      return ViewerEnum.Redoc;
    }
    return ViewerEnum.Swagger;
  }
}
