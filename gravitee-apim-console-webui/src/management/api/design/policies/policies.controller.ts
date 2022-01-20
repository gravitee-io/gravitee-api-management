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
import angular from 'angular';
import * as _ from 'lodash';

class ApiPoliciesController {
  private apiPoliciesByPath: any;
  private policiesToCopy: any[];
  private policiesMap: any;
  private selectedApiPolicy: any;
  private httpMethods: string[];
  private httpMethodsFilter: string[];
  private pathsToCompare: any;
  private dndEnabled: boolean;
  private pathsInitialized: any;
  private httpMethodsUpdated: boolean;
  private schemaByPolicyId: any;

  constructor(
    private ApiService,
    private PolicyService,
    private $mdDialog: angular.material.IDialogService,
    private NotificationService,
    private $scope,
    private dragularService,
    private $q,
    private $rootScope,
    private StringService,
    private UserService,
    private $state: StateService,
  ) {
    'ngInject';

    this.pathsInitialized = [];
    this.dndEnabled = UserService.isUserHasPermissions(['api-definition-u']);

    this.apiPoliciesByPath = {};
    this.policiesToCopy = [];
    this.policiesMap = {};
    this.selectedApiPolicy = {};
    this.httpMethods = ['GET', 'POST', 'PUT', 'DELETE', 'HEAD', 'PATCH', 'OPTIONS', 'TRACE', 'CONNECT'];
    this.httpMethodsFilter = _.clone(this.httpMethods);
    this.httpMethodsUpdated = false;
    this.schemaByPolicyId = {};

    this.listAllPolicies().then((policies) => {
      _.forEach(policies, ({ policy }) => {
        this.policiesToCopy.push(policy);
        this.policiesMap[policy.policyId] = policy;
      });
      _.forEach(this.$scope.$parent.apiCtrl.api.paths, (policies, path) => {
        this.apiPoliciesByPath[path] = _.cloneDeep(policies);
      });
      this.completeApiPolicies(this.apiPoliciesByPath);
      this.initDragular();
      this.pathsToCompare = this.generatePathsToCompare();
    });

    this.$scope.$on(
      'dragulardrop',
      (event, element, dropzoneElt, draggableElt, draggableObjList, draggableIndex, dropzoneObjList, dropzoneIndex) => {
        if (dropzoneObjList !== null) {
          // Automatically display the configuration associated to the dragged policy
          this.editPolicy(dropzoneIndex, dropzoneElt.attributes['data-path'].value).then((schema) => {
            // Automatically save if there is no json schema configuration attached to the dragged policy.
            if (schema.id === 'empty') {
              this.savePaths();
            }
          });
        } else {
          this.savePaths();
        }
      },
    );
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
          if (property !== 'methods' && property !== 'enabled' && property !== 'description' && property !== '$$hashKey') {
            policy.policyId = property;
            const currentPolicy = this.policiesMap[policy.policyId];
            if (currentPolicy) {
              policy.name = currentPolicy.name;
              policy.type = currentPolicy.type;
              policy.version = currentPolicy.version;
              policy.schema = currentPolicy.schema;
            }
          }
        });

        if (!policy.methods) {
          policy.methods = _.clone(this.httpMethods);
        } else {
          policy.methods = _.map(policy.methods, (method: string) => {
            return method.toUpperCase();
          });
        }
      });
    });
  }

  initDragular() {
    const dragularSrcOptions = document.querySelector('.gravitee-policy-draggable');

    this.dragularService([dragularSrcOptions], {
      moves: function () {
        return true;
      },
      copy: true,
      scope: this.$scope,
      containersModel: this.policiesToCopy,
      classes: {
        unselectable: 'gravitee-policy-draggable-selected',
      },
      nameSpace: 'policies',
      accepts: this.acceptDragDrop,
    });
  }

  initDragularDropZone(path) {
    if (!this.pathsInitialized[path]) {
      const dragularApiOptions = document.querySelector('.dropzone-' + this.StringService.hashCode(path));
      if (dragularApiOptions) {
        this.dragularService([dragularApiOptions], {
          moves: function () {
            return true;
          },
          copy: false,
          scope: this.$scope,
          containersModel: this.apiPoliciesByPath[path],
          classes: {
            unselectable: 'gravitee-policy-draggable-selected',
          },
          nameSpace: 'policies',
          accepts: this.acceptDragDrop,
        });
        this.pathsInitialized[path] = true;
      }
    }
  }

  listAllPolicies() {
    return this.PolicyService.list({ expandSchema: true }).then((policies) => {
      return _.map(policies.data, (originalPolicy: any) => {
        const policy = {
          policyId: originalPolicy.id,
          methods: this.httpMethods,
          version: originalPolicy.version,
          name: originalPolicy.name,
          type: originalPolicy.type,
          description: originalPolicy.description,
          enabled: originalPolicy.enabled || true,
        };
        return { policy };
      });
    });
  }

  acceptDragDrop(el, target, source) {
    const draggable = document.querySelector('.gravitee-policy-draggable');
    return source === draggable || source === target;
  }

  editPolicy(index, path) {
    this.$scope.policyJsonSchemaForm = ['*'];
    this.selectedApiPolicy = this.apiPoliciesByPath[path][index];
    if (this.schemaByPolicyId[this.selectedApiPolicy.policyId] === undefined) {
      return this.PolicyService.getSchema(this.selectedApiPolicy.policyId).then((response) => {
        this.$scope.policyJsonSchema = this.schemaByPolicyId[this.selectedApiPolicy.policyId] = response.data;
        this.selectedApiPolicy[this.selectedApiPolicy.policyId] = this.selectedApiPolicy[this.selectedApiPolicy.policyId] || {};
        this.checkEmptySchema();
        return this.$scope.policyJsonSchema;
      });
    } else {
      return this.$q(() => {
        this.$scope.policyJsonSchema = this.schemaByPolicyId[this.selectedApiPolicy.policyId];
        this.selectedApiPolicy[this.selectedApiPolicy.policyId] = this.selectedApiPolicy[this.selectedApiPolicy.policyId] || {};
        this.checkEmptySchema();
        return this.$scope.policyJsonSchema;
      });
    }
  }

  getHttpMethodClass(method, methods) {
    return 'gravitee-policy-method-badge-' + method + (methods.indexOf(method) > -1 ? '-selected' : '-unselected');
  }

  getApiPolicyClass(policy) {
    const classes = [];
    // eslint-disable-next-line angular/no-private-call
    const selected = this.selectedApiPolicy && this.selectedApiPolicy.$$hashKey === policy.$$hashKey;
    if (selected) {
      classes.push('gravitee-policy-card-selected');
    }

    if (!selected && !policy.enabled) {
      classes.push('gravitee-policy-card-disabled');
    }

    if (!policy.name) {
      classes.push('gravitee-policy-card-missed');
    }
    return classes.join(' ');
  }

  getDropzoneClass(path) {
    return 'gravitee-policy-dropzone ' + 'gravitee-policy-dropzone-filled' + ' dropzone-' + this.StringService.hashCode(path);
  }

  toggleHttpMethod(method, methods) {
    this.httpMethodsUpdated = true;
    const index = methods.indexOf(method);
    if (index > -1) {
      methods.splice(index, 1);
    } else {
      methods.push(method);
    }
  }

  filterByMethod(policy) {
    return _.reduce(
      _.map(policy.methods, (method: string) => {
        return this.httpMethodsFilter.indexOf(method) < 0;
      }),
      (result, n) => {
        return result && n;
      },
    );
  }

  removePolicy(index, path, ev) {
    ev.stopPropagation();
    this.selectedApiPolicy = null;
    // eslint-disable-next-line angular/no-private-call
    const hashKey = this.apiPoliciesByPath[path][index].$$hashKey;
    this.$mdDialog
      .show({
        controller: 'DialogConfirmController',
        controllerAs: 'ctrl',
        template: require('../../../../components/dialog/confirmWarning.dialog.html'),
        clickOutsideToClose: true,
        locals: {
          title: 'Are you sure you want to remove this policy?',
          confirmButton: 'Remove',
        },
      })
      .then((response) => {
        if (response) {
          _.forEach(this.apiPoliciesByPath[path], (policy, idx) => {
            // eslint-disable-next-line angular/no-private-call
            if (policy.$$hashKey === hashKey) {
              this.apiPoliciesByPath[path].splice(idx, 1);
              return false;
            }
          });
          this.savePaths();
        }
      });
  }

  editPolicyDescription(index, path, ev) {
    ev.stopPropagation();
    this.selectedApiPolicy = null;

    const policy = this.apiPoliciesByPath[path][index];
    this.$mdDialog
      .show({
        controller: 'DialogEditPolicyController',
        controllerAs: 'editPolicyDialogCtrl',
        template: require('./dialog/policy.dialog.html'),
        clickOutsideToClose: true,
        locals: {
          description: policy.description,
        },
      })
      .then(
        (description) => {
          policy.description = description;
          this.savePaths();
        },
        () => {
          // You cancelled the dialog
        },
      );
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
    _.forEach(this.$scope.$parent.apiCtrl.api.paths, (policies) => {
      _.forEach(policies, (policy) => {
        delete policy.policyId;
        delete policy.name;
        delete policy.type;
        delete policy.version;
        delete policy.schema;

        // do not save empty fields on arrays
        _.forOwn(policy, (policyAttributeValueObject) => {
          _.forOwn(policyAttributeValueObject, (policyAttributeAttribute) => {
            if (_.isArray(policyAttributeAttribute)) {
              _.remove(policyAttributeAttribute, (policyAttributeAttributeItem) => {
                return policyAttributeAttributeItem === undefined || '' === policyAttributeAttributeItem;
              });
            }
          });
        });
      });
    });

    const api = this.$scope.$parent.apiCtrl.api;
    return this.ApiService.update(api).then((updatedApi) => {
      this.NotificationService.show("API '" + updatedApi.data.name + "' saved");
      this.pathsToCompare = this.generatePathsToCompare();

      this.httpMethodsUpdated = false;
      this.$rootScope.$broadcast('apiChangeSuccess', { api: updatedApi.data });
    });
  }

  showAddPathModal(event) {
    this.$mdDialog
      .show({
        controller: 'AddPoliciesPathController',
        controllerAs: 'addPoliciesPathCtrl',
        template: require('./addPoliciesPath.html'),
        parent: angular.element(document.body),
        targetEvent: event,
        clickOutsideToClose: true,
        locals: {
          paths: this.apiPoliciesByPath,
          rootCtrl: this,
        },
      })
      .then((paths) => {
        this.apiPoliciesByPath = paths;
        this.savePaths();
      });
  }

  migrateApiToPolicyStudio() {
    this.$mdDialog
      .show({
        controller: 'DialogConfirmController',
        controllerAs: 'ctrl',
        template: require('../../../../components/dialog/confirmWarning.dialog.html'),
        clickOutsideToClose: true,
        locals: {
          title: 'Are you sure you want to migrate to Policy Studio?',
          msg: 'The migration process will save the API definition, but it will not be deployed. You can still do a rollback from history.',
          confirmButton: 'Yes, I want to migrate',
        },
      })
      .then((response) => {
        if (response) {
          this.ApiService.migrateApiToPolicyStudio(this.$scope.$parent.apiCtrl.api.id).then((response) => {
            this.$state.go('management.apis.detail.design.flows', { apiId: response.data.id }, { reload: true });
          });
        }
      });
  }

  removePath(path) {
    this.selectedApiPolicy = {};
    this.$mdDialog
      .show({
        controller: 'DialogConfirmController',
        controllerAs: 'ctrl',
        template: require('../../../../components/dialog/confirmWarning.dialog.html'),
        clickOutsideToClose: true,
        locals: {
          title: 'Are you sure you want to remove this path?',
          confirmButton: 'Remove',
        },
      })
      .then((response) => {
        if (response) {
          delete this.apiPoliciesByPath[path];
          this.pathsInitialized[path] = false;
          this.savePaths();
        }
      });
  }

  pathNotExists(path, index) {
    if (!path || path.trim() === '') {
      return true;
    }

    if (index && this.clearPathParam(path) === this.clearPathParam(this.sortedPaths()[index])) {
      return true;
    }

    return !_.includes(this.pathsToCompare, this.clearPathParam(path));
  }

  pathStartWithSlash(path) {
    if (!path || path.trim() === '') {
      return true;
    }
    return path[0] === '/';
  }

  clearPathParam(path) {
    if (path === '/') {
      return '/';
    } else {
      return path
        .trim()
        .replace(/(:.*?\/)|(:.*$)/g, ':x/')
        .replace(/\/+$/, '');
    }
  }

  sortedPaths() {
    const paths = _.keys(this.apiPoliciesByPath);
    return _.sortBy(paths, (path) => {
      return this.clearPathParam(path);
    });
  }

  pathKeyPress(ev, el, newPath, index) {
    switch (ev.keyCode) {
      case 13: // enter
        if (!el.$invalid) {
          const oldPath: any = this.sortedPaths()[index];
          this.apiPoliciesByPath[newPath] = this.apiPoliciesByPath[oldPath];
          delete this.apiPoliciesByPath[oldPath];
          this.savePaths();
        }
        break;
      case 27: // escape
        this.restoreOldPath(index, el);
        break;
      default:
        break;
    }
  }

  restoreOldPath(index, el) {
    el.$setViewValue(this.sortedPaths()[index]);
    el.$commitViewValue();
    // TODO: check editPathForm on form
    (document.forms as any).editPathForm['path' + index].value = this.sortedPaths()[index];
  }

  private checkEmptySchema() {
    if (!this.$scope.policyJsonSchema || Object.keys(this.$scope.policyJsonSchema).length === 0) {
      this.$scope.policyJsonSchema = {
        type: 'object',
        id: 'empty',
        properties: { '': {} },
      };
    }
    this.httpMethodsUpdated = false;
  }
}

export default ApiPoliciesController;
