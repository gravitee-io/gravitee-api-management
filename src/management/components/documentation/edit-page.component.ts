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
import UserService from "../../../services/user.service";

interface IPageScope extends IScope {
  getContentMode: string;
  fetcherJsonSchema: string;
  rename: boolean;
  editorReadonly: boolean;
  currentTab: string;
}
const EditPageComponent: ng.IComponentOptions = {
  bindings: {
    resolvedPage: '<',
    resolvedGroups: '<',
    resolvedFetchers: '<'
  },
  template: require('./edit-page.html'),
  controller: function (
    NotificationService: NotificationService,
    DocumentationService: DocumentationService,
    UserService: UserService,
    $state: StateService,
    $scope: IPageScope
  ) {
    'ngInject';
    this.apiId = $state.params.apiId;
    this.tabs = ["content", "config", "fetchers", "access-control"];
    const indexOfTab = this.tabs.indexOf($state.params.tab);
    this.selectedTab = indexOfTab > -1 ? indexOfTab : 0;
    this.currentTab = this.tabs[this.selectedTab];

    $scope.rename = false;

    this.$onInit = () => {
      this.page = this.resolvedPage;
      this.groups = this.resolvedGroups;
      this.fetchers = this.resolvedFetchers;
      if( DocumentationService.supportedTypes().indexOf(this.page.type) < 0) {
        $state.go("management.settings.documentation");
      }

      this.emptyFetcher = {
        "type": "object",
        "id": "empty",
        "properties": {"" : {}}
      };
      $scope.fetcherJsonSchema = this.emptyFetcher;
      this.fetcherJsonSchemaForm = ["*"];
      this.initEditor();


      this.codeMirrorOptions = {
        lineWrapping: true,
        lineNumbers: true,
        allowDropFileTypes: true,
        autoCloseTags: true,
        readOnly: $scope.editorReadonly,
        mode: "javascript",
      };

      if (this.apiId) {
        this.canUpdate = UserService.isUserHasPermissions(["api-documentation-u"]);
      } else {
        this.canUpdate = UserService.isUserHasPermissions(["portal-documentation-u"]);
      }
    };

    this.initEditor = () => {
      $scope.editorReadonly = false;
      if(!(_.isNil(this.page.source) || _.isNil(this.page.source.type))) {
        _.forEach(this.fetchers, fetcher => {
          if (fetcher.id === this.page.source.type) {
            $scope.fetcherJsonSchema = JSON.parse(fetcher.schema);
            $scope.editorReadonly = true;
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

    this.removeFetcher = () => {
      this.page.source = null;
      $scope.fetcherJsonSchema = this.emptyFetcher;
    };

    this.save = () => {
      DocumentationService.update(this.page, this.apiId)
        .then( (response) => {
          NotificationService.show("'" + this.page.name + "' has been updated");
          if (this.apiId) {
            $state.go("management.apis.detail.portal.editdocumentation", {pageId: this.page.id, tab: this.currentTab}, {reload: true});
          } else {
            $state.go("management.settings.editdocumentation", {pageId: this.page.id, tab: this.currentTab}, {reload: true});
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
        $state.go("management.apis.detail.portal.documentation", {apiId: this.apiId, parent: this.page.parentId});
      } else {
        $state.go("management.settings.documentation", {parent: this.page.parentId});
      }
    };

    this.reset = () => {
      if (this.apiId) {
        $state.go("management.apis.detail.portal.editdocumentation", {pageId: this.page.id}, {reload: true});
      } else {
        $state.go("management.settings.editdocumentation", {pageId: this.page.id}, {reload: true});
      }
    };

    this.toggleRename = () => {
      $scope.rename = !$scope.rename;
      if ($scope.rename) {
        this.newName = this.page.name;
      }
    };

    this.rename = () => {
      DocumentationService.partialUpdate("name", this.newName, this.page.id, this.apiId).then( () => {
        NotificationService.show("'" + this.page.name + "' has been renamed to '" + this.newName + "'");
        this.page.name = this.newName;
        this.toggleRename();
      });
    };

    this.goToExternalSource = () => {
      this.selectedTab = 2;
    };

    this.selectTab = (idx: number) => {
      this.selectedTab = idx;
      this.currentTab = this.tabs[this.selectedTab];
      if (this.apiId) {
        $state.transitionTo("management.apis.detail.portal.editdocumentation", {apiId: this.apiId, pageId: this.page.id, tab: this.currentTab}, {notify: false});
      } else {
        $state.transitionTo("management.settings.editdocumentation", {pageId: this.page.id, tab: this.currentTab}, {notify: false});
      }
    };

    this.fetch = () => {
      DocumentationService.fetch(this.page.id, this.apiId).then( () => {
        NotificationService.show("'" + this.page.name + "' has been successfully fetched");
        this.reset();
      });
    };

    this.toggleEntrypointAsServer = () => {
      if (this.page.configuration.entrypointsAsServers === 'false') {
        this.page.configuration.entrypointAsBasePath = this.page.configuration.entrypointsAsServers;
      }
    }
  }
};

export default EditPageComponent;
