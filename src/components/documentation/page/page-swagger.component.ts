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
import * as jsyaml from 'js-yaml';
import * as _ from 'lodash';
import UserService from '../../../services/user.service';
import { SwaggerUIBundle } from 'swagger-ui-dist';
import { StateService } from '@uirouter/core';
import { IController } from 'angular';
import angular = require('angular');

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
  page: any;
  edit: boolean;

  cfg: Record<string, unknown>;
  pageId: string;
  url: string;
  constructor(
    private readonly Constants,
    private readonly UserService: UserService,
    private $state: StateService,
    private $window: ng.IWindowService,
  ) {
    'ngInject';

    this.cfg = {
      dom_id: '#swagger-container',
      presets: [SwaggerUIBundle.presets.apis],
      layout: 'BaseLayout',
      // plugins: will be replaced in $onChanges
      requestInterceptor: (req) => {
        if (req.loadSpec) {
          req.credentials = 'include';
        }
        return req;
      },
      // spec: will be replaced in $onChanges
      // oauth2RedirectUrl: will be replaced in $onChanges
    };
  }

  tryItEnabled() {
    return (
      this.page.configuration?.tryIt === 'true' &&
      (this.UserService.isAuthenticated() || this.page.configuration?.tryItAnonymous === 'true')
    );
  }

  loadContent(): any {
    let contentAsJson = {};
    try {
      contentAsJson = angular.fromJson(this.page.content);
    } catch (e) {
      contentAsJson = jsyaml.safeLoad(this.page.content);
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
    this.pageId = this.page === undefined ? this.$state.params.pageId : this.page.id;
    if (this.$state.params.apiId) {
      this.url = this.Constants.env.baseURL + '/apis/' + this.$state.params.apiId + '/pages/' + this.pageId + '/content';
    } else {
      this.url = this.Constants.env.baseURL + '/portal/pages/' + this.pageId + '/content';
    }
    if (this.url.includes('{:envId}')) {
      this.url = this.url.replace('{:envId}', this.Constants.org.currentEnv.id);
    }

    const plugins = this.loadPlugins();
    const spec = this.loadContent();
    const oauth2RedirectUrl = this.loadOauth2RedirectUrl();
    const config: any = Object.assign({}, this.cfg, { plugins, spec, oauth2RedirectUrl });

    if (this.page.configuration?.showURL === 'true') {
      config.url = this.url;
      config.spec = undefined;
    }
    config.docExpansion = this.page.configuration?.docExpansion ?? 'none';
    config.displayOperationId = this.page.configuration?.displayOperationId === 'true';
    config.filter = this.page.configuration?.enableFiltering === 'true';
    config.showExtensions = this.page.configuration?.showExtensions === 'true';
    config.showCommonExtensions = this.page.configuration?.showCommonExtensions === 'true';
    config.maxDisplayedTags =
      _.isNaN(Number(this.page.configuration.maxDisplayedTags)) || this.page.configuration.maxDisplayedTags === '-1'
        ? undefined
        : Number(this.page.configuration.maxDisplayedTags);

    const ui = SwaggerUIBundle(
      _.merge(config, {
        onComplete: () => {
          // May be used in a short future, so keeping this part of the code to not forget about it.
          ui.initOAuth({
            clientId: '',
            //              appName: "Swagger UI",
            scopeSeparator: ' ',
            //              additionalQueryStringParams: {some_parm: "val"}
          });
          //            ui.preauthorizeApiKey('api_key', 'my_api_key')
        },
      }),
    );
  }
}

export const PageSwaggerComponent: ng.IComponentOptions = {
  template: require('./page-swagger.html'),
  bindings: {
    page: '<',
    edit: '<',
  },
  controller: PageSwaggerComponentController,
};
