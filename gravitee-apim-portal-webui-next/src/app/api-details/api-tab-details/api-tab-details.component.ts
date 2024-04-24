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
import { AsyncPipe, DatePipe } from '@angular/common';
import { Component, Input, OnInit } from '@angular/core';
import { map, Observable, of, switchMap } from 'rxjs';

import { MarkdownDescriptionPipe } from '../../../components/pipe/markdown-description.pipe';
import { Api } from '../../../entities/api/api';
import { ConfigService } from '../../../services/config.service';
import { MarkdownService } from '../../../services/markdown.service';
import { PageService } from '../../../services/page.service';

@Component({
  selector: 'app-api-tab-details',
  standalone: true,
  imports: [MarkdownDescriptionPipe, AsyncPipe, DatePipe],
  templateUrl: './api-tab-details.component.html',
  styleUrl: './api-tab-details.component.scss',
})
export class ApiTabDetailsComponent implements OnInit {
  @Input()
  api!: Api;
  content$!: Observable<string | unknown>;

  constructor(
    private pageService: PageService,
    private configurationService: ConfigService,
    private markdownService: MarkdownService,
  ) {}

  ngOnInit(): void {
    this.content$ = this.pageService.listByApiId(this.api.id, true).pipe(
      switchMap(pageResponse => {
        if (pageResponse.data?.length) {
          return this.pageService.content(this.api.id, pageResponse.data[0].id);
        } else {
          return of(null);
        }
      }),
      map(pageWithContent => {
        if (pageWithContent?.content) {
          return this.markdownService.render(pageWithContent.content, this.configurationService.baseURL, 'documentation/:pageId', []);
        } else {
          return null;
        }
      }),
    );
  }
}
