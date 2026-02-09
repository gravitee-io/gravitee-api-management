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
import angular from 'angular';
import { AfterViewInit, Component, ElementRef, Input, OnChanges, ViewChild } from '@angular/core';
import SwaggerUI from 'swagger-ui';
import * as yaml from 'js-yaml';

const yamlSchema = yaml.DEFAULT_SCHEMA.extend([]);

const loadContent = (spec: string) => {
  let contentAsJson = {};
  if (spec) {
    try {
      contentAsJson = angular.fromJson(spec);
    } catch (e) {
      contentAsJson = yaml.load(spec, { schema: yamlSchema });
    }
  }
  return contentAsJson;
};

const loadOauth2RedirectUrl = () => {
  return (
    window.location.origin +
    window.location.pathname +
    (window.location.pathname.substring(-1) !== '/' ? '/' : '') +
    'swagger-oauth2-redirect.html'
  );
};

@Component({
  selector: 'gio-swagger-ui',
  templateUrl: './gio-swagger-ui.component.html',
  styleUrls: ['./gio-swagger-ui.component.scss'],
  standalone: false,
})
export class GioSwaggerUiComponent implements AfterViewInit, OnChanges {
  @Input()
  spec: string;

  @Input()
  docExpansion: 'none' | 'list' | 'full' = 'none';

  @Input()
  displayOperationId = false;

  @Input()
  filter: string | boolean = true;

  @Input()
  showExtensions = true;

  @Input()
  showCommonExtensions = true;

  @Input()
  maxDisplayedTags: number;

  @ViewChild('swagger')
  swaggerNode: ElementRef;

  ngOnChanges(): void {
    if (this.swaggerNode) {
      this.initSwaggerUI();
    }
  }

  ngAfterViewInit(): void {
    this.initSwaggerUI();
  }

  private initSwaggerUI() {
    SwaggerUI({
      domNode: this.swaggerNode.nativeElement,
      spec: loadContent(this.spec),
      docExpansion: this.docExpansion,
      displayOperationId: this.displayOperationId,
      filter: this.filter,
      showExtensions: this.showExtensions,
      showCommonExtensions: this.showCommonExtensions,
      maxDisplayedTags: this.maxDisplayedTags,
      oauth2RedirectUrl: loadOauth2RedirectUrl(),
    });
  }
}
