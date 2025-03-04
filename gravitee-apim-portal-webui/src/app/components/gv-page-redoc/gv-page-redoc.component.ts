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
import { Component, HostListener, Input, OnDestroy, ViewChild, OnInit, ChangeDetectorRef } from '@angular/core';
import { getCssVar } from '@gravitee/ui-components/src/lib/style';

import { NotificationService } from '../../services/notification.service';
import { ScrollService } from '../../services/scroll.service';
import { GvDocumentationComponent } from '../gv-documentation/gv-documentation.component';
import { PageService } from '../../services/page.service';
import { readYaml } from '../../utils/yaml-parser';

declare let Redoc: any;

@Component({
  selector: 'app-gv-page-redoc',
  templateUrl: './gv-page-redoc.component.html',
  styleUrls: ['./gv-page-redoc.component.css'],
})
export class GvPageRedocComponent implements OnInit, OnDestroy {
  isLoaded = false;

  @ViewChild('redoc', { static: true }) redocContainer;

  @Input() fragment: string;

  constructor(
    private cd: ChangeDetectorRef,
    private notificationService: NotificationService,
    private pageService: PageService,
  ) {}

  /**
   * Redoc script is automatically loaded. See `angular.json` scripts section.
   */
  ngOnInit() {
    const page = this.pageService.getCurrentPage();
    this.refresh(page);
  }

  @HostListener('window:resize')
  @HostListener('window:scroll')
  onScroll() {
    window.requestAnimationFrame(() => {
      GvDocumentationComponent.updateMenuPosition(this.redocMenu);
    });
  }

  refresh(page) {
    if (page) {
      // https://github.com/Redocly/redoc/blob/master/src/theme.ts
      const color = getCssVar(document.body, '--gv-theme-color-dark', undefined);
      const successColor = getCssVar(document.body, '--gv-theme-color', undefined);
      const dangerColor = getCssVar(document.body, '--gv-theme-color-danger', undefined);
      const textColor = getCssVar(document.body, '--gv-theme-font-color-dark', undefined);
      const textColorLight = getCssVar(document.body, '--gv-theme-font-color-light', undefined);
      const fontFamily = getCssVar(document.body, '--gv-theme-font-family', undefined);
      const fontSize = getCssVar(document.body, '--gv-theme-font-size-m', undefined);
      const sidebarColor = getCssVar(document.body, '--gv-theme-neutral-color-lightest', undefined);

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
              fontFamily,
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

      let contentAsJson;
      try {
        contentAsJson = JSON.parse(page.content);
      } catch (e) {
        contentAsJson = readYaml(page.content);
      }

      Redoc.init(contentAsJson, options, this.redocContainer.nativeElement, errors => this._redocCallback(errors));
    }
  }

  get redocMenu(): Element {
    return document.querySelector('.menu-content');
  }

  _redocCallback(errors) {
    if (errors) {
      this.notificationService.error('gv-page.swagger.badFormat');
    }
    this.isLoaded = true;
    this.cd.detectChanges();
    setTimeout(() => {
      const top = ScrollService.getHeaderHeight() + GvDocumentationComponent.PAGE_PADDING_TOP_BOTTOM;

      const wrap = document.querySelector('.redoc-wrap');
      // eslint-disable-next-line @typescript-eslint/ban-ts-comment
      // @ts-ignore
      const height = window.getComputedStyle(wrap).height;

      let container = document.querySelector('.page__content');
      if (container == null) {
        container = document.querySelector('.layout__content');
      }
      // eslint-disable-next-line @typescript-eslint/ban-ts-comment
      // @ts-ignore
      container.style.minHeight = height;
      // eslint-disable-next-line @typescript-eslint/ban-ts-comment
      // @ts-ignore
      document.querySelector(GvDocumentationComponent.PAGE_COMPONENT).style.minHeight = height;
      // eslint-disable-next-line @typescript-eslint/ban-ts-comment
      // @ts-ignore
      this.redocMenu.style.position = 'fixed';
      // eslint-disable-next-line @typescript-eslint/ban-ts-comment
      // @ts-ignore
      this.redocMenu.style.height = ` ${window.innerHeight - top - GvDocumentationComponent.PAGE_PADDING_TOP_BOTTOM}px`;
      // eslint-disable-next-line @typescript-eslint/ban-ts-comment
      // @ts-ignore
      this.redocMenu.style.top = `${top}px`;
      // eslint-disable-next-line @typescript-eslint/ban-ts-comment
      // @ts-ignore
      this.redocMenu.style.bottom = `${GvDocumentationComponent.PAGE_PADDING_TOP_BOTTOM}px`;
      const width = window.getComputedStyle(document.querySelector('.menu-content')).width;
      // eslint-disable-next-line @typescript-eslint/ban-ts-comment
      // @ts-ignore
      document.querySelector('.api-content').style.marginLeft = width;
      document.querySelector('.menu-content a[target="_blank"]').remove();
      [].forEach.call(document.querySelectorAll('.api-content h2 a, .api-content h1 a'), link => {
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
    ['.layout__content', '.page__content', GvDocumentationComponent.PAGE_COMPONENT].forEach(xpath => {
      const element = document.querySelector(xpath);
      if (element) {
        // eslint-disable-next-line @typescript-eslint/ban-ts-comment
        // @ts-ignore
        element.style.height = 'auto';
        // eslint-disable-next-line @typescript-eslint/ban-ts-comment
        // @ts-ignore
        element.style.minHeight = 'auto';
      }
    });
  }
}
