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
    this.apiPoliciesByPath = {};
    this.policiesToCopy = [];
    this.policiesMap = {};
    this.selectedApiPolicy = {};
    this.httpMethods = ['GET','POST','PUT','DELETE','HEAD','PATCH','OPTIONS','TRACE','CONNECT'];
    this.httpMethodsFilter = _.clone(this.httpMethods);

    this.listAllPoliciesWithSchema().then( (policiesWithSchema) => {
      _.forEach(policiesWithSchema, ({policy}) => {
        this.policiesToCopy.push(policy);
        this.policiesMap[policy.policyId] = policy;
      });
      _.forEach(resolvedApi.data.paths, (policies, path) => {
        this.apiPoliciesByPath[path] = _.cloneDeep(policies);
      });
      this.completeApiPolicies(this.apiPoliciesByPath);
      this.initDragular();
    });

    const that = this;
    this.$scope.$on('dragulardrop', function(/*event, element, dropzoneElt , draggableElt, draggableObjList, draggableIndex, dropzoneObjList*/) {
      that.savePaths();
    });
  }

  completeApiPolicies(pathMap) {
    _.forEach(pathMap, (policies) => {
      _.forEach(policies, (policy) => {

        _.forEach(policy, (value, property) => {
          if (property !== "methods" && property !== "$$hashKey") {
            policy.policyId = property;
            policy.name = this.policiesMap[policy.policyId].name;
            policy.type = this.policiesMap[policy.policyId].type;
            policy.version = this.policiesMap[policy.policyId].version;
            policy.schema = this.policiesMap[policy.policyId].schema;
          }
        });

        if ( !policy.methods ) {
          policy.methods = _.clone(this.httpMethods);
        } else {
          policy.methods = _.map(policy.methods, (method) => { return method.toUpperCase(); });
        }
      });
    });
  }

  initDragular() {
    const dragularSrcOptions = document.querySelector('.gravitee-policy-draggable');

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
  }

  initDragularDropZone(path) {
    const dragularApiOptions = document.querySelector('.dropzone-' + _.kebabCase(path));
    this.DragularService([dragularApiOptions], {
      copy: false,
      scope: this.$scope,
      containersModel: this.apiPoliciesByPath[path],
      classes: {
        unselectable: 'gravitee-policy-draggable-selected'
      },
      nameSpace: 'policies',
      accepts: this.acceptDragDrop
    });
    return true;
  }

  listAllPoliciesWithSchema() {
    return this.PolicyService.list({expandSchema: true}).then( (policyServiceListResponse) => {

      const promises = _.map(policyServiceListResponse.data, (originalPolicy) => {
        return this.PolicyService.getSchema(originalPolicy.id).then( ({data}) => {
          return {
            schema: data,
            originalPolicy
          };
        }
        , (response) => {
            if ( response.status === 404) {
              return {
                schema: {},
                originalPolicy
              }
            } else {
              //todo manage errors
              console.log(response)
            }
          });
      });

      return this.$q.all(promises).then( (policySchemaResponses) => {
        return _.map(policySchemaResponses, ({schema, originalPolicy}) => {
          const policy = {
            policyId: originalPolicy.id,
            methods: this.httpMethods,
            version: originalPolicy.version,
            name: originalPolicy.name,
            type: originalPolicy.type,
            description: originalPolicy.description,
            schema
          };
          policy[originalPolicy.id] = {};
          _.forEach(schema.properties, (value, property) => {
            policy[originalPolicy.id][property] = null;
          });
          return {policy};
        });
      });
    });
  }

  acceptDragDrop(el, target, source) {
    const draggable = document.querySelector('.gravitee-policy-draggable');
    return ( (source === draggable || source === target) && el.id !== "api-key");
  }

  editPolicy(index, path) {
    if ( this.apiPoliciesByPath[path][index].policyId === 'api-key' ) {
      this.selectedApiPolicy = {};
    } else {
      this.selectedApiPolicy = this.apiPoliciesByPath[path][index];
      this.$scope.policyJsonSchema = this.selectedApiPolicy.schema;
      this.$scope.policyJsonSchemaForm = ["*"];
    }
  }

  getHttpMethodClass(method, methods) {
    return "gravitee-policy-method-badge-" + method +
      (methods.indexOf(method) > -1 ? "-selected" : "-unselected");
  }

  getApiPolicyClass(policy) {
    return this.selectedApiPolicy && this.selectedApiPolicy.$$hashKey === policy.$$hashKey ? "gravitee-policy-card-selected" : "";
  }

  getDropzoneClass(path) {
    return "gravitee-policy-dropzone "
      + ((this.apiPoliciesByPath[path].length < 2) ? '': 'gravitee-policy-dropzone-filled')
      + " dropzone-" + _.kebabCase(path);
  }

  toggleHttpMethod(method, methods) {
    const index = methods.indexOf(method);
    if ( index > -1 ) {
      methods.splice(index, 1);
    } else {
      methods.push(method);
    }
  }

  filterByMethod(policy) {
    return _.reduce(
      _.map(policy.methods, (method) => {
        return this.httpMethodsFilter.indexOf(method) < 0;
      }), (result, n) => { return result && n; });
  }

  removePolicy(path) {
    let alert = this.$mdDialog.confirm({
      title: 'Warning',
      content: 'Are you sure you want to remove this policy ?',
      ok: 'OK',
      cancel: 'Cancel'
    });

    const that = this;

    this.$mdDialog
      .show(alert)
      .then(function () {
        _.forEach(that.apiPoliciesByPath[path], (policy, idx) => {
          if ( policy.$$hashKey === that.selectedApiPolicy.$$hashKey ) {
            that.apiPoliciesByPath[path].splice(idx, 1);
            that.selectedApiPolicy = null;
            return false;
          }
        });
        that.savePaths();
      });
  }

  savePaths() {
    this.$scope.$parent.apiCtrl.api.paths = _.cloneDeep(this.apiPoliciesByPath);
    _.forEach(this.$scope.$parent.apiCtrl.api.paths, (policies, path) => {
      _.forEach(this.$scope.$parent.apiCtrl.api.paths[path], (policy, idx) => {
        delete this.$scope.$parent.apiCtrl.api.paths[path][idx].policyId;
        delete this.$scope.$parent.apiCtrl.api.paths[path][idx].name;
        delete this.$scope.$parent.apiCtrl.api.paths[path][idx].type;
        delete this.$scope.$parent.apiCtrl.api.paths[path][idx].description;
        delete this.$scope.$parent.apiCtrl.api.paths[path][idx].version;
        delete this.$scope.$parent.apiCtrl.api.paths[path][idx].schema;
      });
    });

    const that = this;
    return this.ApiService.update(this.$scope.$parent.apiCtrl.api).then( ( {data} ) => {
      that.$scope.$parent.apiCtrl.api = data;
      that.NotificationService.show('API \'' + that.$scope.$parent.apiCtrl.api.name + '\' saved');
    });
  }

  showAddPathModal(event) {
    this.$mdDialog.show({
      controller: 'AddPoliciesPathController',
      controllerAs: 'addPoliciesPathCtrl',
      templateUrl: 'app/api/admin/policies/addPoliciesPath.html',
      parent: angular.element(document.body),
      targetEvent: event,
      clickOutsideToClose: true,
      paths: this.apiPoliciesByPath,
      apiKeyPolicy: this.policiesMap['api-key']
    }).then( (paths) => {
      this.apiPoliciesByPath = paths;
      this.savePaths();
    });
  }

  removePath(path) {
    let alert = this.$mdDialog.confirm({
      title: 'Warning',
      content: 'Are you sure you want to remove this path ?',
      ok: 'OK',
      cancel: 'Cancel'
    });

    const that = this;

    this.$mdDialog
      .show(alert)
      .then(function () {
        delete that.apiPoliciesByPath[path];
        that.savePaths();
      });
  }

  sortedPaths() {
    let paths = _.keys(this.apiPoliciesByPath);
    return _.sortBy(paths, (path) => {
      return path;
    });
  }
}

export default ApiPoliciesController;
