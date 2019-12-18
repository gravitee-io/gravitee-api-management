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
import { Component, OnInit, ViewChild, ComponentFactoryResolver, Input, OnChanges, SimpleChanges } from '@angular/core';
import { Page, PageConfiguration } from '@gravitee/ng-portal-webclient';

import { GvPageContentSlotDirective } from 'src/app/directives/gv-page-content-slot.directive';
import { GvPageSwaggerUIComponent } from '../gv-page-swaggerui/gv-page-swaggerui.component';
import { GvPageRedocComponent } from '../gv-page-redoc/gv-page-redoc.component';
import { GvPageMarkdownComponent } from '../gv-page-markdown/gv-page-markdown.component';

@Component({
  selector: 'app-gv-page',
  templateUrl: './gv-page.component.html',
  styleUrls: ['./gv-page.component.css']
})
export class GvPageComponent implements OnChanges {

  @ViewChild(GvPageContentSlotDirective, { static: true }) appGvPageContentSlot: GvPageContentSlotDirective;
  @Input () page: Page;


  constructor(
    private componentFactoryResolver: ComponentFactoryResolver,
    ) { }

  ngOnChanges(changes: SimpleChanges) {
    if (changes.page) {
      this._loadViewer();
    }
  }

  private _loadViewer() {
    let componentFactory;
    if (this.page && this.page.type.toUpperCase() === Page.TypeEnum.MARKDOWN) {
      componentFactory = this.componentFactoryResolver.resolveComponentFactory(GvPageMarkdownComponent);
    } else {
      const documentationViewer = (this.page.configuration ? this.page.configuration.viewer : '');
      if (documentationViewer && documentationViewer.toUpperCase() === PageConfiguration.ViewerEnum.Redoc.toUpperCase()) {
        componentFactory = this.componentFactoryResolver.resolveComponentFactory(GvPageRedocComponent);
      } else {
        componentFactory = this.componentFactoryResolver.resolveComponentFactory(GvPageSwaggerUIComponent);
      }
    }
    this.appGvPageContentSlot.viewContainerRef.clear();
    const viewerPage: any = this.appGvPageContentSlot.viewContainerRef.createComponent(componentFactory);
    viewerPage.instance.page = this.page;
  }
}
