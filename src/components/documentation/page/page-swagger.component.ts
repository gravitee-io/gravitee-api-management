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

const DisableTryItOutPlugin = function () {
  return {
    statePlugins: {
      spec: {
        wrapSelectors: {
          allowTryItOutFor: () => () => false
        }
      }
    }
  };
};

const PageSwaggerComponent: ng.IComponentOptions = {
  template: require('./page-swagger.html'),
  bindings: {
    page: '<',
    edit: '<'
  },
  controller: function (Constants, UserService: UserService, $state: StateService) {
    'ngInject';

    this.$onChanges = () => {
      this.pageId = (this.page === undefined) ? $state.params.pageId : this.page.id;
      if ($state.params.apiId) {
        this.url = Constants.envBaseURL + '/apis/' + $state.params.apiId + '/pages/' + this.pageId + '/content';
      } else {
        this.url = Constants.envBaseURL + '/portal/pages/' + this.pageId + '/content';
      }

      this.tryItEnabled = () => {
        return !_.isNil(this.page.configuration) && this.page.configuration.tryIt === 'true' &&
          (UserService.isAuthenticated() || this.page.configuration.tryItAnonymous === 'true');
      };

      const plugins = [];
      if (!this.tryItEnabled()) {
        plugins.push(DisableTryItOutPlugin);
      }

      let contentAsJson = {};
      try {
        contentAsJson = JSON.parse(this.page.content);
      } catch (e) {
        contentAsJson = jsyaml.safeLoad(this.page.content);
      }

      let cfg: any = {
        dom_id: '#swagger-container',
        presets: [
          SwaggerUIBundle.presets.apis,
        ],
        layout: 'BaseLayout',
        plugins: plugins,
        requestInterceptor: (req) => {
          if (req.loadSpec) {
            req.credentials = 'include';
          }
          return req;
        },
        spec: contentAsJson,
        oauth2RedirectUrl: window.location.origin + window.location.pathname + (window.location.pathname.substr(-1) !== '/' ? '/' : '') + 'swagger-oauth2-redirect.html',
      };

      if (!_.isNil(this.page.configuration)) {
        if (this.page.configuration.showURL === 'true') {
          cfg.url = this.url;
          cfg.spec = undefined;
        }
        cfg.docExpansion =
          _.isNil(this.page.configuration.docExpansion)
            ? 'none' : this.page.configuration.docExpansion;
        cfg.displayOperationId =
          _.isNil(this.page.configuration.displayOperationId)
            ? false : this.page.configuration.displayOperationId === 'true';
        cfg.filter =
          _.isNil(this.page.configuration.enableFiltering)
            ? false : this.page.configuration.enableFiltering === 'true';
        cfg.showExtensions =
          _.isNil(this.page.configuration.showExtensions)
            ? false : this.page.configuration.showExtensions === 'true';
        cfg.showCommonExtensions =
          _.isNil(this.page.configuration.showCommonExtensions)
            ? false : this.page.configuration.showCommonExtensions === 'true';
        cfg.maxDisplayedTags =
          _.isNaN(Number(this.page.configuration.maxDisplayedTags)) || this.page.configuration.maxDisplayedTags === '-1'
            ? undefined : Number(this.page.configuration.maxDisplayedTags);
      }

      const ui = SwaggerUIBundle(
        _.merge(cfg, {
          onComplete: () => {
            // May be used in a short future, so keeping this part of the code to not forget about it.
            ui.initOAuth({
              clientId: '',
//              appName: "Swagger UI",
              scopeSeparator: ' ',
//              additionalQueryStringParams: {some_parm: "val"}
            });
//            ui.preauthorizeApiKey('api_key', 'my_api_key')
          }
        }));
    };

  }
};

export default PageSwaggerComponent;
