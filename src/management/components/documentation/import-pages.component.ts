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

import NotificationService from "../../../services/notification.service";
import DocumentationService from "../../../services/documentation.service";
import {StateService} from "@uirouter/core";
import {IScope} from "angular";
import _ = require('lodash');

interface IPageScope extends IScope {
  fetcherJsonSchema: string;
}
const ImportPagesComponent: ng.IComponentOptions = {
  bindings: {
    resolvedFetchers: '<',
    resolvedRootPage: '<'
  },
  template: require('./import-pages.html'),
  controller: function (
    NotificationService: NotificationService,
    DocumentationService: DocumentationService,
    $state: StateService,
    $scope: IPageScope
  ) {
    'ngInject';
    this.apiId = $state.params.apiId;

    this.codeMirrorOptions = {
      lineWrapping: true,
      lineNumbers: true,
      allowDropFileTypes: true,
      autoCloseTags: true,
      mode: "javascript"
    };

    this.$onInit = () => {

      this.page = this.resolvedRootPage || {
        name: "root",
        type: "ROOT"
      };

      this.fetchers = this.resolvedFetchers;

      this.emptyFetcher = {
        "type": "object",
        "id": "empty",
        "properties": {"" : {}}
      };
      $scope.fetcherJsonSchema = this.emptyFetcher;
      this.fetcherJsonSchemaForm = ["*"];

      if(!(_.isNil(this.page.source) || _.isNil(this.page.source.type))) {
        _.forEach(this.fetchers, fetcher => {
          if (fetcher.id === this.page.source.type) {
            $scope.fetcherJsonSchema = JSON.parse(fetcher.schema);
          }
        });
      }
    };

    this.configureFetcher = (fetcher) => {
      if (! this.page.source) {
        this.page.source = {};
      }

      this.page.source = {
        type: fetcher.id,
        configuration: {}
      };
      $scope.fetcherJsonSchema = JSON.parse(fetcher.schema);
    };

    this.import = () => {
      this.page.name="import";
      DocumentationService.import(this.page, this.apiId)
        .then( (response: any) => {
          if (this.page.id) {
            NotificationService.show("'" + response.data.length + "' elements has been updated.");
          } else {
            NotificationService.show("'" + response.data.length + "' elements has been created.");
          }
          if (this.apiId) {
            $state.go("management.apis.detail.portal.documentation", {apiId: this.apiId});
          } else {
            $state.go("management.settings.documentation");
          }
      });
    };

    this.changeContentMode = (newMode) => {
      if ("fetcher" === newMode) {
        this.page.source = {
          configuration: {}
        };
      } else {
        delete this.page.source;
      }
    };

    this.cancel = () => {
      if (this.apiId) {
        $state.go("management.apis.detail.portal.documentation", {apiId: this.apiId});
      } else {
        $state.go("management.settings.documentation");
      }
    };
  }
};

export default ImportPagesComponent;
