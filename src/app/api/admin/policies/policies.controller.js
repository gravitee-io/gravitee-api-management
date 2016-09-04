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
  constructor (ApiService, resolvedApi, PolicyService, $state, $mdDialog, NotificationService, $scope, dragularService, $q, $rootScope) {
    'ngInject';
    this.ApiService = ApiService;
    this.PolicyService = PolicyService;
    this.DragularService = dragularService;
    this.$mdDialog = $mdDialog;
    this.NotificationService = NotificationService;
    this.$scope = $scope;
    this.$state = $state;
    this.$q = $q;
    this.$rootScope = $rootScope;
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
      this.pathsToCompare = this.generatePathsToCompare();
    });

    const that = this;
    this.$scope.$on('dragulardrop', function(event, element, dropzoneElt , draggableElt, draggableObjList, draggableIndex, dropzoneObjList, dropzoneIndex) {

      var policy = dropzoneObjList[dropzoneIndex];
      // Automatically save if there is no json schema configuration attached to the dragged policy.
      if (policy.schema === undefined || policy.schema === '') {
        that.savePaths();
      }
    });
  }

  generatePathsToCompare() {
    return _.map(_.keys(this.apiPoliciesByPath), (p) => {
      return this.clearPathParam(p);
    });
  }

  completeApiPolicies(pathMap) {
    _.forEach(pathMap, (policies) => {
      _.forEach(policies, (policy) => {

        _.forEach(policy, (value, property) => {
          if (property !== "methods" && property !== "enabled" && property !== "description" && property !== "$$hashKey") {
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
    if (dragularApiOptions) {
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
    return false;
  }

  listAllPoliciesWithSchema() {
    return this.PolicyService.list({expandSchema: true}).then( (policyServiceListResponse) => {

      const promises = _.map(policyServiceListResponse.data, (originalPolicy) => {
        return this.PolicyService.getSchema(originalPolicy.id).then( ({data}) => {
          return {
            schema: data,
            originalPolicy
          };
        },
          (response) => {
            if ( response.status === 404) {
              return {
                schema: {},
                originalPolicy
              };
            } else {
              //todo manage errors
              console.log(response);
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
            enabled: originalPolicy.enabled || true,
            schema
          };
          policy[originalPolicy.id] = {};

          return {policy};
        });
      });
    });
  }

  acceptDragDrop(el, target, source) {
    const draggable = document.querySelector('.gravitee-policy-draggable');
    return ( (source === draggable || source === target) && el.id !== "api-key");
  }

  editPolicy(index, path, ev) {
    ev.stopPropagation();
    this.selectedApiPolicy = this.apiPoliciesByPath[path][index];
    this.$scope.policyJsonSchema = this.selectedApiPolicy.schema;
    if (Object.keys(this.$scope.policyJsonSchema).length == 0) {
      this.$scope.policyJsonSchema = {
        "type": "object",
        "id": "empty",
        "properties": {"" : {}}
      };
    }
    this.$scope.policyJsonSchemaForm = ["*"];
  }

  getHttpMethodClass(method, methods) {
    return "gravitee-policy-method-badge-" + method +
      (methods.indexOf(method) > -1 ? "-selected" : "-unselected");
  }

  getApiPolicyClass(policy) {
    const classes = [];
    const selected = this.selectedApiPolicy && this.selectedApiPolicy.$$hashKey === policy.$$hashKey;
    if (selected) {
      classes.push("gravitee-policy-card-selected");
    }

    if (!selected && ! policy.enabled) {
      classes.push("gravitee-policy-card-disabled");
    }
    return classes.join();
  }

  getDropzoneClass(path) {
    return "gravitee-policy-dropzone " +
      'gravitee-policy-dropzone-filled' +
      " dropzone-" + _.kebabCase(path);
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

  removePolicy(index, path, ev) {
    ev.stopPropagation();
    this.selectedApiPolicy = null;
    const hashKey = this.apiPoliciesByPath[path][index].$$hashKey;
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
          if ( policy.$$hashKey === hashKey ) {
            that.apiPoliciesByPath[path].splice(idx, 1);
            return false;
          }
        });
        that.savePaths();
      });
  }

  editPolicyDescription(index, path, ev) {
    ev.stopPropagation();
    this.selectedApiPolicy = null;

    const policy = this.apiPoliciesByPath[path][index];
    const that = this;

    this.$mdDialog.show({
      controller: 'DialogEditPolicyController',
      controllerAs: 'editPolicyDialogCtrl',
      templateUrl: 'app/api/admin/policies/dialog/policy.dialog.html',
      locals: {
        description: policy.description
      }
    }).then(function (description) {
      policy.description = description;
      that.savePaths();
    }, function() {
      // You cancelled the dialog
    });
  }

  switchPolicyEnabled(index, path, ev) {
    ev.stopPropagation();
    this.selectedApiPolicy = null;

    const policy = this.apiPoliciesByPath[path][index];
    policy.enabled = !policy.enabled;
    this.savePaths();
  }

  savePaths() {
    this.$scope.$parent.apiCtrl.api.paths = _.cloneDeep(this.apiPoliciesByPath);
    _.forEach(this.$scope.$parent.apiCtrl.api.paths, (policies, path) => {
      _.forEach(this.$scope.$parent.apiCtrl.api.paths[path], (policy, idx) => {
        delete this.$scope.$parent.apiCtrl.api.paths[path][idx].policyId;
        delete this.$scope.$parent.apiCtrl.api.paths[path][idx].name;
        delete this.$scope.$parent.apiCtrl.api.paths[path][idx].type;
      //  delete this.$scope.$parent.apiCtrl.api.paths[path][idx].description;
        delete this.$scope.$parent.apiCtrl.api.paths[path][idx].version;
        delete this.$scope.$parent.apiCtrl.api.paths[path][idx].schema;
      });
    });

    const that = this;
    return this.ApiService.update(this.$scope.$parent.apiCtrl.api).then( ( {data} ) => {
      that.$scope.$parent.apiCtrl.api = data;
      that.$rootScope.$broadcast('apiChangeSuccess');
      that.NotificationService.show('API \'' + that.$scope.$parent.apiCtrl.api.name + '\' saved');
      this.pathsToCompare = this.generatePathsToCompare();
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
      apiKeyPolicy: this.policiesMap['api-key'],
      rootCtrl: this
    }).then( (paths) => {
      this.apiPoliciesByPath = paths;
      this.savePaths();
    });
  }

  removePath(path) {
    this.selectedApiPolicy = {};
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

  pathNotExists(path, index) {
    if (!path || path.trim() === "") {
      return true;
    }
    if(index && this.clearPathParam(path) === this.clearPathParam(this.sortedPaths()[index])) {
      return true;
    }

    return !_.includes(this.pathsToCompare, this.clearPathParam(path));
  }

  pathStartWithSlash(path) {
    if (!path || path.trim() === "") {
      return true;
    }
    return path[0] === "/";
  }

  clearPathParam(path) {
    if ( path === "/" ) {
      return "/";
    } else {
      return path.trim().replace(/(:.*?\/)|(:.*$)/g, ":x\/").replace(/\/+$/, "");
    }
  }

  sortedPaths() {
    let paths = _.keys(this.apiPoliciesByPath);
    return _.sortBy(paths, (path) => {
      return this.clearPathParam(path);
    });
  }

  pathKeyPress(ev, el, newPath, index) {
    switch (ev.keyCode) {
      case 13: //enter
        if (!el.$invalid) {
          const oldPath = this.sortedPaths()[index];
          this.apiPoliciesByPath[newPath] = this.apiPoliciesByPath[oldPath];
          delete this.apiPoliciesByPath[oldPath];
          this.savePaths();
        }
        break;
      case 27: //escape
        this.restoreOldPath(index, el);
        break;
      default:
        break;
    }
  }

  restoreOldPath(index, el) {
    el.$setViewValue(this.sortedPaths()[index]);
    el.$commitViewValue();
    document.forms.editPathForm['path'+index].value = this.sortedPaths()[index];
  }
}

export default ApiPoliciesController;
