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
import { StateService } from '@uirouter/core';
import { IController } from 'angular';
import angular = require('angular');
import * as jsyaml from 'js-yaml';
import * as _ from 'lodash';
import { SwaggerUIBundle } from 'swagger-ui-dist';

import UserService from '../../../services/user.service';

const yamlSchema = jsyaml.Schema.create(jsyaml.FAILSAFE_SCHEMA, []);

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

  cfg: Record<string, unknown>;
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
      this.pageConfiguration?.tryIt === 'true' && (this.UserService.isAuthenticated() || this.pageConfiguration?.tryItAnonymous === 'true')
    );
  }

  loadContent(): any {
    let contentAsJson = {};
    try {
      contentAsJson = angular.fromJson(this.pageContent);
    } catch (e) {
      contentAsJson = jsyaml.safeLoad(this.pageContent, { schema: yamlSchema });
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
    const config: any = Object.assign({}, this.cfg, { plugins, spec, oauth2RedirectUrl });

    if (this.pageConfiguration?.showURL === 'true') {
      let url = '';
      if (this.$state.params.apiId) {
        url = `${this.Constants.env.baseURL}/apis/${this.$state.params.apiId}/pages/${this.$state.params.pageId}/content'`;
      } else {
        url = `${this.Constants.env.baseURL}/portal/pages/${this.$state.params.pageId}/content'`;
      }
      if (url.includes('{:envId}')) {
        url = url.replace('{:envId}', this.Constants.org.currentEnv.id);
      }

      config.url = url;
      config.spec = undefined;
    }
    config.docExpansion = this.pageConfiguration?.docExpansion ?? 'none';
    config.displayOperationId = this.pageConfiguration?.displayOperationId === 'true';
    config.filter = this.pageConfiguration?.enableFiltering === 'true';
    config.showExtensions = this.pageConfiguration?.showExtensions === 'true';
    config.showCommonExtensions = this.pageConfiguration?.showCommonExtensions === 'true';
    config.maxDisplayedTags =
      _.isNaN(Number(this.pageConfiguration.maxDisplayedTags)) || this.pageConfiguration.maxDisplayedTags === '-1'
        ? undefined
        : Number(this.pageConfiguration.maxDisplayedTags);

    const swaggerRenderer = SwaggerUIBundle(
      _.merge(config, {
        onComplete: () => {
          // May be used in a short future, so keeping this part of the code to not forget about it.
          swaggerRenderer.initOAuth({
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
    pageConfiguration: '<',
    pageContent: '<',
    edit: '<',
  },
  controller: PageSwaggerComponentController,
};
