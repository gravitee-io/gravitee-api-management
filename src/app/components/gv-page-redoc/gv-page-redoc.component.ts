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
import { AfterViewInit, Component, Input, OnDestroy, OnInit, ViewChild } from '@angular/core';
import { ApiService, Page, PortalService } from '@gravitee/ng-portal-webclient';
import { marker as i18n } from '@biesbjerg/ngx-translate-extract-marker';
import { NotificationService } from '../../services/notification.service';
import { ActivatedRoute, Router } from '@angular/router';
import { getCssVar } from '@gravitee/ui-components/src/lib/style';

declare let Redoc: any;

@Component({
  selector: 'app-gv-page-redoc',
  templateUrl: './gv-page-redoc.component.html',
  styleUrls: ['./gv-page-redoc.component.css']
})
export class GvPageRedocComponent implements OnDestroy {

  isLoaded = false;

  @ViewChild('redoc', { static: true }) redocContainer;

  @Input() fragment: string;

  @Input() set page(page: Page) {
    if (page) {
      this.loadScript()
        .then(() => {
          const apiId = this.route.snapshot.params.apiId;
          const pageId = page.id;
          if (apiId) {
            return this.apiService.getPageByApiIdAndPageId({ apiId, pageId, include: ['content'] }).toPromise();
          } else {
            return this.portalService.getPageByPageId({ pageId, include: ['content'] }).toPromise();
          }
        })
        .then((response) => {
          this.refresh(response);
        });
    }
  }

  constructor(
    private portalService: PortalService,
    private apiService: ApiService,
    private notificationService: NotificationService,
    private router: Router,
    private route: ActivatedRoute,
  ) {

  }

  loadScript() {
    return new Promise(resolve => {
      const scriptId = 'redoc-standalone';
      if (document.getElementById(scriptId) == null) {
        const scriptElement = document.createElement('script');
        scriptElement.async = true;
        scriptElement.src = 'redoc.js';
        scriptElement.onload = resolve;
        scriptElement.id = scriptId;
        document.body.appendChild(scriptElement);
      } else {
        resolve();
      }
    });
  }

  refresh(page) {
    // @ts-ignore
    // https://github.com/Redocly/redoc/blob/master/src/theme.ts
    const color = getCssVar(document.body, '--gv-theme-color-dark');
    const successColor = getCssVar(document.body, '--gv-theme-color');
    const dangerColor = getCssVar(document.body, '--gv-theme-color-danger');
    const textColor = getCssVar(document.body, '--gv-theme-font-color-dark');
    const textColorLight = getCssVar(document.body, '--gv-theme-font-color-light');
    const fontFamily = getCssVar(document.body, '--gv-theme-font-family');
    const fontSize = getCssVar(document.body, '--gv-theme-font-size-m');
    const sidebarColor = getCssVar(document.body, '--gv-theme-neutral-color-lightest');

    const options = {
      expandResponses: '',
      expandSingleSchemaField: false,
      lazyRendering: true,
      scrollYOffset: 120,
      theme: {
        colors: {
          primary: { main: color },
          success: { main: successColor },
          error: { main: dangerColor },
          text: { primary: textColor },
        },
        typography: {
          fontSize,
          fontFamily,
          headings: {
            fontFamily
          },
        },
        sidebar: {
          backgroundColor: sidebarColor,
          textColor,
        },
        rightPanel: {
          backgroundColor: color,
          textColor: textColorLight,
        },

      },
    };

    Redoc.init(page._links.content, options, this.redocContainer.nativeElement, (errors) => this._redocCallback(errors));

  }

  _redocCallback(errors) {
    if (errors) {
      this.notificationService.error(i18n('gv-page.swagger.badFormat'));
    }
    this.isLoaded = true;
    setTimeout(() => {
      const wrap = document.querySelector('.redoc-wrap');
      const height = window.getComputedStyle(wrap).height;
      // @ts-ignore
      document.querySelector('.layout__content').style.height = height;
      // @ts-ignore
      document.querySelector('.gv-documentation__content').style.height = height;
      const menu = document.querySelector('.menu-content');
      // @ts-ignore
      menu.style.position = 'fixed';
      // @ts-ignore
      menu.style.top = '132px';
      const width = window.getComputedStyle(document.querySelector('.menu-content')).width;
      // @ts-ignore
      document.querySelector('.api-content').style.marginLeft = width;
      document.querySelector('.menu-content a[target="_blank"]').remove();
      [].forEach.call(document.querySelectorAll('.api-content h2 a, .api-content h1 a'), (link) => {
        link.remove();
      });
      if (this.fragment) {
        setTimeout(() => {
          const elt = document.getElementById(`${this.fragment}`);
          if (elt) {
            elt.scrollIntoView({ block: 'start', inline: 'nearest' });
          }
        }, 350);
      }
    }, 0);
  }


  ngOnDestroy() {
    ['.layout__content', '.gv-documentation__content'].forEach((xpath) => {
      const element = document.querySelector(xpath);
      if (element) {
        // @ts-ignore
        element.style.height = 'auto';
      }
    });
  }

}
