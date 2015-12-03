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
  constructor (ApiService, resolvedApi, PolicyService, $state, $mdDialog, NotificationService, $scope, dragularService, $q) {
    'ngInject';
    this.ApiService = ApiService;
    this.PolicyService = PolicyService;
    this.DragularService = dragularService;
    this.$mdDialog = $mdDialog;
    this.NotificationService = NotificationService;
    this.$scope = $scope;
    this.$state = $state;
    this.$q = $q;
    this.resolvedApi = resolvedApi;
    this.apiPoliciesByPath = new Map();
    if ( resolvedApi.data.paths["/*"] ) {
      this.apiPoliciesByPath.set("/*", _.cloneDeep(resolvedApi.data.paths["/*"]));
    } else {
      this.apiPoliciesByPath.set("/*", []);
    }
    this.completeApiPolicies(this.apiPoliciesByPath);
    this.policySchemaMap = new Map();
    this.policyMap = new Map();
    this.policiesToCopy = [];

    this.selectedApiPolicy = {};
    this.listAllPoliciesWithSchema().then( () => {
      this.initDragular()
    });
    this.httpMethods = ['GET','POST','PUT','DELETE','HEAD','PATCH','OPTIONS','TRACE','CONNECT'];
    this.httpMethodsFilter = _.clone(this.httpMethods);
  }

  completeApiPolicies(pathMap) {
    for ( var pathPolicies of pathMap.values()) {
      for ( var apiPolicy of pathPolicies ) {
        for (var property of Object.keys(apiPolicy)) {
          if (property !== "methods" && property !== "$$hashKey") {
            apiPolicy.policyId = property;
          }
        }
        if ( !apiPolicy.methods ) {
          apiPolicy.methods = _.clone(this.httpMethods);
        }
        //TODO hack temporaire à supprimer lors de la mise a jour de demo
        else {
          for ( var idx = 0 ; idx < apiPolicy.methods.length; idx++ ) {
            apiPolicy.methods[idx] = apiPolicy.methods[idx].toUpperCase();
          }
        }
      }
    }
  }

  initDragular() {
    var dragularSrcOptions= document.querySelector('.gravitee-policy-draggable'),
      dragularApiOptions = document.querySelector('.gravitee-policy-dropzone');

    this.DragularService([dragularSrcOptions], {
      copy: true,
      scope: this.$scope,
      containersModel: this.policiesToCopy,
      classes: {
        unselectable: 'gravitee-policy-draggable-selected'
      },
      nameSpace: 'policies',
      accepts: this.acceptDragDrop
    });

    this.DragularService([dragularApiOptions], {
      copy: false,
      scope: this.$scope,
      containersModel: this.apiPoliciesByPath.get("/*"),
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
    return this.$q( (resolve) => {
      resolve(
        this.PolicyService.list().then(response => {
          for (var originalPolicy of response.data) {
            this.policyMap.set(originalPolicy.id, originalPolicy);
            ((service, originalPolicy, policiesToCopy, httpMethods, policySchemaMap) => {
              service.getSchema(originalPolicy.id).then(response => {
                var policy = {};
                policy.policyId = originalPolicy.id;
                policy.methods = httpMethods;
                policy[originalPolicy.id] = {};
                var properties = Object.keys(response.data.properties);
                for (var property of properties) {
                  policy[originalPolicy.id][property] = null;
                }
                policySchemaMap.set(originalPolicy.id, response.data.properties);
                policiesToCopy.push(policy);
              });
            })(this.PolicyService, originalPolicy, this.policiesToCopy, this.httpMethods, this.policySchemaMap);
          }
        }))
    });
  }

  acceptDragDrop(el, target, source) {
    var draggable = document.querySelector('.gravitee-policy-draggable');
    return (source === draggable || source === target);
  }

  editPolicy(index) {
    this.selectedApiPolicy = this.apiPoliciesByPath.get("/*")[index];
  }

  getHttpMethodClass(method, methods) {
    return "gravitee-policy-method-badge-" + method +
      (methods.indexOf(method) > -1 ? "-selected" : "-unselected");
  }

  getDropzoneClass() {
    return "gravitee-policy-dropzone " +((this.apiPoliciesByPath.get('/*').length < 2) ? '': 'gravitee-policy-dropzone-filled');
  }

  toggleHttpMethod(method, methods) {
    var index = methods.indexOf(method);
    if ( index > -1 ) {
      methods.splice(index, 1);
    } else {
      methods.push(method);
    }
  }

  filterByMethod(policy) {
    for ( var method of policy.methods ) {
      if ( this.httpMethodsFilter.indexOf(method) > -1 ) {
        return false;
      }
    }
    return true;
  }

  removePolicy() {
    var alert = this.$mdDialog.confirm({
      title: 'Warning',
      content: 'Are you sure you want to remove this policy ?',
      ok: 'OK',
      cancel: 'Cancel'
    });

    var that = this;

    this.$mdDialog
      .show(alert)
      .then(function () {
        for ( var idx = 0; idx < that.apiPoliciesByPath.get("/*").length; idx++ ) {
          if ( that.apiPoliciesByPath.get("/*")[idx].$$hashKey === that.selectedApiPolicy.$$hashKey ) {
            that.apiPoliciesByPath.get("/*").splice(idx, 1);
            that.selectedApiPolicy = null;
          }
        }
      });
  }

  savePaths() {
    this.$scope.$parent.apiCtrl.api.paths["/*"] = _.cloneDeep(this.apiPoliciesByPath.get("/*"));
    for ( var policy of this.$scope.$parent.apiCtrl.api.paths["/*"] ) {
      delete policy.policyId;
    }
    this.ApiService.update(this.$scope.$parent.apiCtrl.api);
  }
/*
  reset() {
    this.apiPoliciesByPath = new Map();
    this.apiPoliciesByPath.set("/*", _.cloneDeep(this.resolvedApi.data.paths["/*"]));
    this.completeApiPolicies(this.apiPoliciesByPath);
    this.initDragular();
    this.selectedApiPolicy = {};
  }
  */
}

export default ApiPoliciesController;
