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
/* global document:false */
class ApiPoliciesController {
  constructor (ApiService, resolvedApi, PolicyService, $state, $mdDialog, NotificationService, $scope) {
    'ngInject';
    this.ApiService = ApiService;
    this.PolicyService = PolicyService;
    this.$mdDialog = $mdDialog;
    this.NotificationService = NotificationService;
    this.$scope = $scope;
    this.$state = $state;
    this.api = resolvedApi.data;

    this.policies = [];
    this.apiPolicies = [];
    this.selectApiPolicy = {};
    this.listAllPolicies();
    $scope.dragularSrcOptions = {
      copy: true,
      containersModel: this.policies,
      classes: {
        unselectable: 'gravitee-policy-draggable-selected'
      },
      nameSpace: 'policies',
      accepts: this.acceptDragDrop
    };
    $scope.dragularApiOptions = {
      copy: true,
      containersModel: this.apiPolicies,
      classes: {
        unselectable: 'gravitee-policy-draggable-selected'
      },
      nameSpace: 'policies',
      accepts: this.acceptDragDrop
    };
    this.selectedPolicy = null;
  }

  listAllPolicies() {
    /*this.PolicyService.list().then(response => {
     this.policies = response.data;
     });*/
    this.policies = [
      {
        "id": "rate-limit",
        "name": "Rate Limit",
        "description": "Description of the Rate Limit Gravitee Policy",
        "version": "0.1.0-SNAPSHOT"
      },
      {
        "id": "cors",
        "name": "Cors",
        "description": "Description of the Cors Gravitee Policy",
        "version": "0.1.0-SNAPSHOT"
      },
      {
        "id": "api-key",
        "name": "ApiKey",
        "description": "Description of the ApiKey Gravitee Policy",
        "version": "0.1.0-SNAPSHOT"
      }
    ];
  }

  acceptDragDrop(el, target, source) {
    var draggable = document.querySelector('.gravitee-policy-draggable');
    return (source === draggable || source === target);
  }

  listPolicies(apiName) {
    this.ApiService.listPolicies(apiName).then(response => {
      // TODO filter request, response and request/response policies
      this.policies = {
        'OnRequest': response.data,
        'OnResponse': [],
        'OnRequest/OnResponse': []
      };
    });
  }

  displayPolicyToConfigure(policy) {
    this.selectApiPolicy = policy;
  }
}

export default ApiPoliciesController;
