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
import { marker as i18n } from '@biesbjerg/ngx-translate-extract-marker';
import { NotificationService } from '../../services/notification.service';
import { ActivatedRoute } from '@angular/router';

declare let Redoc: any;

@Component({
  selector: 'app-gv-page',
  templateUrl: './gv-page.component.html',
  styleUrls: ['./gv-page.component.css']
})
export class GvPageComponent {
  isSwaggerParsing = false;
  currentPage: Page;

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
    private notificationService: NotificationService,
    private route: ActivatedRoute,
    ) { }

  isMarkdown(page: Page) {
    return page && page.type.toUpperCase() === Page.TypeEnum.MARKDOWN;
  }

  isSwagger(page: Page) {
    return page && page.type.toUpperCase() === Page.TypeEnum.SWAGGER;
  }

  refresh(page: Page) {
    if (this.isSwagger(page)) {
      this.isSwaggerParsing = true;
      let redocElement = document.getElementById('redoc');
      if (!redocElement) {
        redocElement = document.createElement('div');
        redocElement.setAttribute('id', 'redoc');
        document.querySelector('.gv-page-container').appendChild(redocElement);
      }
      // @ts-ignore
      Redoc.init(page._links.content, { }, document.getElementById('redoc'),
        (errors) => {
          if (errors) {
            document.querySelector('.gv-page-container').removeChild(redocElement);
            this.notificationService.error(i18n('gv-page.swagger.badFormat'));
          } else {
            this.currentPage = page;
            this.isSwaggerParsing = false;
          }
        }
      );
    } else {
      this.isSwaggerParsing = false;
      this.currentPage = page;
      const redocTag = document.getElementById('redoc');
      if (redocTag) {
        document.querySelector('.gv-page-container').removeChild(redocTag);
      }
    }
  }
}
