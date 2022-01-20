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
import angular, { IController, IScope } from 'angular';
import * as _ from 'lodash';

import { DocumentationService } from '../../services/documentation.service';
import NotificationService from '../../services/notification.service';

interface IPageScope extends IScope {
  fetcherJsonSchema: string;
}

class ImportPagesComponentController implements IController {
  resolvedFetchers: any[];
  resolvedRootPage: any;

  apiId: string;
  page: any;
  fetchers: any[];
  importInProgress: boolean;
  constructor(
    private readonly NotificationService: NotificationService,
    private readonly DocumentationService: DocumentationService,
    private $state: StateService,
    private $scope: IPageScope,
  ) {
    'ngInject';
    this.apiId = this.$state.params.apiId;
  }

  $onInit() {
    this.page = this.resolvedRootPage || {
      name: 'root',
      type: 'ROOT',
    };

    this.fetchers = this.resolvedFetchers;

    if (!(_.isNil(this.page.source) || _.isNil(this.page.source.type))) {
      _.forEach(this.fetchers, (fetcher) => {
        if (fetcher.id === this.page.source.type) {
          this.$scope.fetcherJsonSchema = angular.fromJson(fetcher.schema);
        }
      });
    }
  }

  import() {
    this.importInProgress = true;
    this.page.name = 'import';
    this.DocumentationService.import(this.page, this.apiId)
      .then((response: any) => {
        if (this.page.id) {
          if (response.data.messages && response.data.messages.length > 0) {
            this.NotificationService.showError(
              "'" +
                response.data.length +
                "' elements has been updated (with validation errors - check the bottom of the page for details)",
            );
          } else {
            this.NotificationService.show("'" + response.data.length + "' elements has been updated.");
          }
        } else {
          if (response.data.messages && response.data.messages.length > 0) {
            this.NotificationService.showError(
              "'" +
                response.data.length +
                "' elements has been created (with validation errors - check the bottom of the page for details)",
            );
          } else {
            this.NotificationService.show("'" + response.data.length + "' elements has been created.");
          }
        }
        if (this.apiId) {
          this.$state.go('management.apis.detail.portal.documentation', { apiId: this.apiId });
        } else {
          this.$state.go('management.settings.documentation');
        }
      })
      .finally(() => {
        this.importInProgress = false;
      });
  }

  cancel() {
    if (this.apiId) {
      this.$state.go('management.apis.detail.portal.documentation', { apiId: this.apiId });
    } else {
      this.$state.go('management.settings.documentation');
    }
  }
}

export const ImportPagesComponent: ng.IComponentOptions = {
  bindings: {
    resolvedFetchers: '<',
    resolvedRootPage: '<',
  },
  template: require('./import-pages.html'),
  controller: ImportPagesComponentController,
};
