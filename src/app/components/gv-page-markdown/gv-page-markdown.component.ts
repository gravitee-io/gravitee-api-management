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
import { Component, Input, OnInit } from '@angular/core';
import { ApiService, Page, PortalService } from '@gravitee/ng-portal-webclient';
import { ActivatedRoute } from '@angular/router';

@Component({
  selector: 'app-gv-page-markdown',
  templateUrl: './gv-page-markdown.component.html',
  styleUrls: ['./gv-page-markdown.component.css']
})
export class GvPageMarkdownComponent implements OnInit {

  @Input() set page(page: Page) {
    if (page) {
      const apiId = this.route.snapshot.params.apiId;
      if (apiId) {
        this.apiService.getPageByApiIdAndPageId({ apiId, pageId: page.id, include: ['content'] }).subscribe(response => {
          this.refresh(response);
        });
      } else {
        this.portalService.getPageByPageId({ pageId: page.id, include: ['content'] }).subscribe(response => {
          this.refresh(response);
        });
      }
    }
  }

  constructor(
    private portalService: PortalService,
    private apiService: ApiService,
    private route: ActivatedRoute,
    ) { }

  currentPage: Page;

  ngOnInit() { }

  refresh(page: Page) {
    this.currentPage = page;
  }
}
