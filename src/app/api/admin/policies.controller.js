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
  constructor (ApiService, resolvedApi, PolicyService, $state, $mdDialog, NotificationService, $scope, dragularService) {
    'ngInject';
    this.ApiService = ApiService;
    this.PolicyService = PolicyService;
    this.DragularService = dragularService;
    this.$mdDialog = $mdDialog;
    this.NotificationService = NotificationService;
    this.$scope = $scope;
    this.$state = $state;
    this.api = resolvedApi.data;

    this.policies = [];
    this.apiPolicies = [];
    this.policiesSchemaMap = new Map();
    this.selectApiPolicy = {};
    this.listAllPoliciesWithSchema();
  }

  initDragular() {
    var dragularSrcOptions= document.querySelector('.gravitee-policy-draggable'),
      dragularApiOptions = document.querySelector('.gravitee-policy-dropzone');
    this.DragularService([dragularSrcOptions], {
      copy: true,
      scope: this.$scope,
      containersModel: this.policies,
      classes: {
        unselectable: 'gravitee-policy-draggable-selected'
      },
      nameSpace: 'policies',
      accepts: this.acceptDragDrop
    });

    this.DragularService([dragularApiOptions], {
      copy: true,
      scope: this.$scope,
      containersModel: this.apiPolicies,
      classes: {
        unselectable: 'gravitee-policy-draggable-selected'
      },
      nameSpace: 'policies',
      accepts: this.acceptDragDrop
    });

    this.$scope.$on('dragulardrop', function(e, el, a , z, r ,t ,y ) {
      console.log("drop", e, el, a , z, r ,t ,y );

    });
  }

  listAllPoliciesWithSchema() {
    this.PolicyService.list().then(response => {
      this.policies = response.data;
      this.initDragular();
      for (var policy of this.policies) {
        this.PolicyService.getSchema(policy.id).then(response => {
          this.policiesSchemaMap.set(policy.id, response.data);
        });
      }
     });
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
