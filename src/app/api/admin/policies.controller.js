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
    this.selectedApiPolicy = {};
    this.selectedApiPolicyProperties = [];
    this.listAllPoliciesWithSchema().then(
      this.initDragular()
    );
    this.httpVerbs = ['get','post','put','delete','head','patch','options','trace','connect'];
    this.defaultPolicyValues = { values: {
      methods: this.httpVerbs
    }};
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
      copy: false,
      scope: this.$scope,
      containersModel: this.apiPolicies,
      classes: {
        unselectable: 'gravitee-policy-draggable-selected'
      },
      nameSpace: 'policies',
      accepts: this.acceptDragDrop
    });

    /*this.$scope.$on('dragulardrop', function(event, element, dropzoneElt , draggableElt, draggableObjList, draggableIndex, dropzoneObjList) {
      console.log(draggableObjList ,"\n", draggableIndex ,"\n", dropzoneObjList );
    });*/
  }

  listAllPoliciesWithSchema() {
    return Promise.resolve(
      this.PolicyService.list().then(response => {
        for (var originalPolicy of response.data) {
          ((service, originalPolicy, policies, defaultValues) => {
            service.getSchema(originalPolicy.id).then(response => {
              var schema = { schema: response.data };
              var properties = Object.keys(response.data.properties);
              var valuesObj = {};
              angular.copy(defaultValues, valuesObj);
              for ( var property of properties ) {
                valuesObj.values[property] = null;
              }
              policies.push(Object.assign({}, originalPolicy, schema, valuesObj));
            });
          })(this.PolicyService, originalPolicy, this.policies, this.defaultPolicyValues);
        }
      }));
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

  editPolicy(index) {
    this.selectedApiPolicy = this.apiPolicies[index];
    this.selectedApiPolicyProperties = Object.keys(this.selectedApiPolicy.schema.properties);
  }

  getHttpVerbClass(verb) {
    return "gravitee-policy-method-badge-" +
      (this.selectedApiPolicy.values.methods.indexOf(verb) > -1 ? "selected" : "unselected");
  }

  toggleHttpVerb(verb) {
    var index = this.selectedApiPolicy.values.methods.indexOf(verb);
    if ( index > -1 ) {
      this.selectedApiPolicy.values.methods.splice(index, 1);
    } else {
      this.selectedApiPolicy.values.methods.push(verb);
    }
  }
}

export default ApiPoliciesController;
