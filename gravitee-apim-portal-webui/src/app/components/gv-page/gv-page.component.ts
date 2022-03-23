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
import {
  Component,
  ComponentFactoryResolver,
  EventEmitter,
  Input,
  OnChanges,
  OnDestroy,
  Output,
  SimpleChanges,
  ViewChild,
} from '@angular/core';
import { ActivatedRoute } from '@angular/router';

import { ApiService, Page, PageConfiguration, PortalService } from '../../../../projects/portal-webclient-sdk/src/lib';
import { GvPageContentSlotDirective } from '../../directives/gv-page-content-slot.directive';
import { PageService } from '../../services/page.service';

@Component({
  selector: 'app-gv-page',
  templateUrl: './gv-page.component.html',
  styleUrls: ['./gv-page.component.css'],
})
export class GvPageComponent implements OnChanges, OnDestroy {
  @ViewChild(GvPageContentSlotDirective, { static: true }) appGvPageContentSlot: GvPageContentSlotDirective;

  @Output() loaded = new EventEmitter<boolean>();

  @Input() page: Page;

  @Input() withToc: boolean;

  @Input() fragment: string;

  constructor(
    private componentFactoryResolver: ComponentFactoryResolver,
    private portalService: PortalService,
    private apiService: ApiService,
    private route: ActivatedRoute,
    private pageService: PageService,
  ) {}

  ngOnChanges(changes: SimpleChanges) {
    if (changes.page) {
      this._loadViewer();
    }
  }

  ngOnDestroy() {
    this.pageService.disposePage();
  }

  private async _loadViewer() {
    if (this.page) {
      let componentFactory;
      if (this.page.type.toUpperCase() === Page.TypeEnum.MARKDOWN) {
        const { GvPageMarkdownComponent } = await import('../gv-page-markdown/gv-page-markdown.component');
        componentFactory = this.componentFactoryResolver.resolveComponentFactory(GvPageMarkdownComponent);
      } else if (this.page.type.toUpperCase() === Page.TypeEnum.SWAGGER) {
        const documentationViewer = this.page.configuration ? this.page.configuration.viewer : '';
        if (documentationViewer && documentationViewer.toUpperCase() === PageConfiguration.ViewerEnum.Redoc.toUpperCase()) {
          const { GvPageRedocComponent } = await import('../gv-page-redoc/gv-page-redoc.component');
          componentFactory = this.componentFactoryResolver.resolveComponentFactory(GvPageRedocComponent);
        } else {
          const { GvPageSwaggerUIComponent } = await import('../gv-page-swaggerui/gv-page-swaggerui.component');
          componentFactory = this.componentFactoryResolver.resolveComponentFactory(GvPageSwaggerUIComponent);
        }
      } else if (this.page.type.toUpperCase() === Page.TypeEnum.ASCIIDOC) {
        const { GvPageAsciiDocComponent } = await import('../gv-page-asciidoc/gv-page-asciidoc.component');
        componentFactory = this.componentFactoryResolver.resolveComponentFactory(GvPageAsciiDocComponent);
      } else if (this.page.type.toUpperCase() === Page.TypeEnum.ASYNCAPI) {
        const { GvPageAsyncApiComponent } = await import('../gv-page-asyncapi/gv-page-asyncapi.component');
        componentFactory = this.componentFactoryResolver.resolveComponentFactory(GvPageAsyncApiComponent);
      }
      this.appGvPageContentSlot.viewContainerRef.clear();
      if (componentFactory) {
        this._loadPageWithContent(this.page).then(page => {
          this.pageService.disposePage();
          const viewerPage: any = this.appGvPageContentSlot.viewContainerRef.createComponent(componentFactory);
          this.pageService.set(page);
          viewerPage.instance.fragment = this.fragment;
          viewerPage.instance.withToc = this.withToc;
          this.loaded.emit(true);
        });
      }
    }
  }

  private _loadPageWithContent(page: Page): Promise<Page> {
    const apiId = this.route.snapshot.params.apiId;
    if (apiId) {
      return this.apiService.getPageByApiIdAndPageId({ apiId, pageId: page.id, include: ['content'] }).toPromise();
    } else {
      return this.portalService.getPageByPageId({ pageId: page.id, include: ['content'] }).toPromise();
    }
  }
}
