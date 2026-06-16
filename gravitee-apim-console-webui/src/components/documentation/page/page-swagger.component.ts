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
import angular, { IController } from 'angular';

import * as yaml from 'js-yaml';
import { isNaN } from 'lodash';
import SwaggerUI from 'swagger-ui';

import UserService from '../../../services/user.service';

const yamlSchema = yaml.DEFAULT_SCHEMA.extend([]);

const OAS_SCHEMA_TYPES = new Set(['null', 'boolean', 'object', 'array', 'number', 'string', 'integer']);
const OAS_TYPE_PRIORITY = ['string', 'number', 'integer', 'boolean', 'array', 'object'];

const normalizeTypeArrays = (obj: any): any => {
  if (obj === null || !angular.isObject(obj)) {
    return obj;
  }
  if (Array.isArray(obj)) {
    return obj.map((item: any) => normalizeTypeArrays(item));
  }
  const result: any = {};
  for (const key of Object.keys(obj)) {
    if (key === 'type' && Array.isArray(obj[key]) && (obj[key] as string[]).every((t: string) => OAS_SCHEMA_TYPES.has(t))) {
      const nonNullTypes = (obj[key] as string[]).filter((t: string) => t !== 'null');
      result[key] = nonNullTypes.length === 1 ? nonNullTypes[0] : (OAS_TYPE_PRIORITY.find((t) => nonNullTypes.includes(t)) ?? 'string');
      if ((obj[key] as string[]).includes('null')) {
        result['nullable'] = true;
      }
    } else {
      result[key] = normalizeTypeArrays(obj[key]);
    }
  }
  return result;
};

const DisableTryItOutPlugin = function () {
  return {
    statePlugins: {
      spec: {
        wrapSelectors: {
          allowTryItOutFor: () => () => false,
        },
      },
    },
  };
};
class PageSwaggerComponentController implements IController {
  pageConfiguration: any;
  pageContent: string;
  edit: boolean;

  constructor(
    private readonly UserService: UserService,
    private $window: ng.IWindowService,
  ) {}

  tryItEnabled() {
    return (
      this.pageConfiguration?.tryIt === 'true' && (this.UserService.isAuthenticated() || this.pageConfiguration?.tryItAnonymous === 'true')
    );
  }

  loadContent(): any {
    let contentAsJson = {};
    try {
      contentAsJson = normalizeTypeArrays(angular.fromJson(this.pageContent));
    } catch (e) {
      contentAsJson = normalizeTypeArrays(yaml.load(this.pageContent, { schema: yamlSchema }));
    }
    return contentAsJson;
  }

  loadOauth2RedirectUrl(): string {
    return (
      this.$window.location.origin +
      this.$window.location.pathname +
      (this.$window.location.pathname.substr(-1) !== '/' ? '/' : '') +
      'swagger-oauth2-redirect.html'
    );
  }

  loadPlugins(): any[] {
    const plugins = [];
    if (!this.tryItEnabled()) {
      plugins.push(DisableTryItOutPlugin);
    }
    return plugins;
  }

  $onChanges() {
    const plugins = this.loadPlugins();
    const spec = this.loadContent();
    const oauth2RedirectUrl = this.loadOauth2RedirectUrl();
    const config: any = { plugins, spec, oauth2RedirectUrl };

    config.docExpansion = this.pageConfiguration?.docExpansion ?? 'none';
    config.displayOperationId = this.pageConfiguration?.displayOperationId === 'true';
    config.filter = this.pageConfiguration?.enableFiltering === 'true';
    config.showExtensions = this.pageConfiguration?.showExtensions === 'true';
    config.showCommonExtensions = this.pageConfiguration?.showCommonExtensions === 'true';
    config.maxDisplayedTags =
      isNaN(Number(this.pageConfiguration?.maxDisplayedTags)) || this.pageConfiguration?.maxDisplayedTags === '-1'
        ? undefined
        : Number(this.pageConfiguration?.maxDisplayedTags);

    SwaggerUI({
      dom_id: '#swagger-container',
      ...config,
    });
  }
}
PageSwaggerComponentController.$inject = ['UserService', '$window'];

export const PageSwaggerComponent: ng.IComponentOptions = {
  template: require('html-loader!./page-swagger.html').default, // eslint-disable-line @typescript-eslint/no-var-requires
  bindings: {
    pageConfiguration: '<',
    pageContent: '<',
    edit: '<',
  },
  controller: PageSwaggerComponentController,
};
