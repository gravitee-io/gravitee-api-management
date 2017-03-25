"use strict";
Object.defineProperty(exports, "__esModule", { value: true });
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
var _ = require("lodash");
var angular = require("angular");
var ApiPoliciesController = (function () {
    function ApiPoliciesController(ApiService, resolvedApi, PolicyService, $mdDialog, NotificationService, $scope, dragularService, $q, $rootScope) {
        'ngInject';
        var _this = this;
        this.ApiService = ApiService;
        this.resolvedApi = resolvedApi;
        this.PolicyService = PolicyService;
        this.$mdDialog = $mdDialog;
        this.NotificationService = NotificationService;
        this.$scope = $scope;
        this.dragularService = dragularService;
        this.$q = $q;
        this.$rootScope = $rootScope;
        this.apiPoliciesByPath = {};
        this.policiesToCopy = [];
        this.policiesMap = {};
        this.selectedApiPolicy = {};
        this.httpMethods = ['GET', 'POST', 'PUT', 'DELETE', 'HEAD', 'PATCH', 'OPTIONS', 'TRACE', 'CONNECT'];
        this.httpMethodsFilter = _.clone(this.httpMethods);
        this.listAllPoliciesWithSchema().then(function (policiesWithSchema) {
            _.forEach(policiesWithSchema, function (_a) {
                var policy = _a.policy;
                _this.policiesToCopy.push(policy);
                _this.policiesMap[policy.policyId] = policy;
            });
            _.forEach(resolvedApi.data.paths, function (policies, path) {
                _this.apiPoliciesByPath[path] = _.cloneDeep(policies);
            });
            _this.completeApiPolicies(_this.apiPoliciesByPath);
            _this.initDragular();
            _this.pathsToCompare = _this.generatePathsToCompare();
        });
        var that = this;
        this.$scope.$on('dragulardrop', function (event, element, dropzoneElt, draggableElt, draggableObjList, draggableIndex, dropzoneObjList, dropzoneIndex) {
            if (dropzoneObjList !== null) {
                var policy = dropzoneObjList[dropzoneIndex];
                // Automatically display the configuration associated to the dragged policy
                that.editPolicy(dropzoneIndex, dropzoneElt.attributes['data-path'].value, event);
                // Automatically save if there is no json schema configuration attached to the dragged policy.
                if (policy.schema === undefined || policy.schema === '') {
                    that.savePaths();
                }
            }
            else {
                that.savePaths();
            }
        });
    }
    ApiPoliciesController.prototype.generatePathsToCompare = function () {
        var _this = this;
        return _.map(_.keys(this.apiPoliciesByPath), function (p) {
            return _this.clearPathParam(p);
        });
    };
    ApiPoliciesController.prototype.completeApiPolicies = function (pathMap) {
        var _this = this;
        _.forEach(pathMap, function (policies) {
            _.forEach(policies, function (policy) {
                _.forEach(policy, function (value, property) {
                    if (property !== "methods" && property !== "enabled" && property !== "description" && property !== "$$hashKey") {
                        policy.policyId = property;
                        policy.name = _this.policiesMap[policy.policyId].name;
                        policy.type = _this.policiesMap[policy.policyId].type;
                        policy.version = _this.policiesMap[policy.policyId].version;
                        policy.schema = _this.policiesMap[policy.policyId].schema;
                    }
                });
                if (!policy.methods) {
                    policy.methods = _.clone(_this.httpMethods);
                }
                else {
                    policy.methods = _.map(policy.methods, function (method) { return method.toUpperCase(); });
                }
            });
        });
    };
    ApiPoliciesController.prototype.initDragular = function () {
        var dragularSrcOptions = document.querySelector('.gravitee-policy-draggable');
        this.dragularService([dragularSrcOptions], {
            copy: true,
            scope: this.$scope,
            containersModel: this.policiesToCopy,
            classes: {
                unselectable: 'gravitee-policy-draggable-selected'
            },
            nameSpace: 'policies',
            accepts: this.acceptDragDrop
        });
    };
    ApiPoliciesController.prototype.initDragularDropZone = function (path) {
        var dragularApiOptions = document.querySelector('.dropzone-' + _.kebabCase(path));
        if (dragularApiOptions) {
            this.dragularService([dragularApiOptions], {
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
    };
    ApiPoliciesController.prototype.listAllPoliciesWithSchema = function () {
        var _this = this;
        return this.PolicyService.list({ expandSchema: true }).then(function (policyServiceListResponse) {
            var promises = _.map(policyServiceListResponse.data, function (originalPolicy) {
                return _this.PolicyService.getSchema(originalPolicy.id).then(function (_a) {
                    var data = _a.data;
                    return {
                        schema: data,
                        originalPolicy: originalPolicy
                    };
                }, function (response) {
                    if (response.status === 404) {
                        return {
                            schema: {},
                            originalPolicy: originalPolicy
                        };
                    }
                    else {
                        //todo manage errors
                    }
                });
            });
            return _this.$q.all(promises).then(function (policySchemaResponses) {
                return _.map(policySchemaResponses, function (_a) {
                    var schema = _a.schema, originalPolicy = _a.originalPolicy;
                    var policy = {
                        policyId: originalPolicy.id,
                        methods: _this.httpMethods,
                        version: originalPolicy.version,
                        name: originalPolicy.name,
                        type: originalPolicy.type,
                        description: originalPolicy.description,
                        enabled: originalPolicy.enabled || true,
                        schema: schema
                    };
                    policy[originalPolicy.id] = {};
                    return { policy: policy };
                });
            });
        });
    };
    ApiPoliciesController.prototype.acceptDragDrop = function (el, target, source) {
        var draggable = document.querySelector('.gravitee-policy-draggable');
        return (source === draggable || source === target);
    };
    ApiPoliciesController.prototype.editPolicy = function (index, path, ev) {
        ev.stopPropagation();
        this.selectedApiPolicy = this.apiPoliciesByPath[path][index];
        this.$scope.policyJsonSchema = this.selectedApiPolicy.schema;
        if (Object.keys(this.$scope.policyJsonSchema).length === 0) {
            this.$scope.policyJsonSchema = {
                "type": "object",
                "id": "empty",
                "properties": { "": {} }
            };
        }
        this.$scope.policyJsonSchemaForm = ["*"];
    };
    ApiPoliciesController.prototype.getHttpMethodClass = function (method, methods) {
        return "gravitee-policy-method-badge-" + method +
            (methods.indexOf(method) > -1 ? "-selected" : "-unselected");
    };
    ApiPoliciesController.prototype.getApiPolicyClass = function (policy) {
        var classes = [];
        var selected = this.selectedApiPolicy && this.selectedApiPolicy.$$hashKey === policy.$$hashKey;
        if (selected) {
            classes.push("gravitee-policy-card-selected");
        }
        if (!selected && !policy.enabled) {
            classes.push("gravitee-policy-card-disabled");
        }
        return classes.join();
    };
    ApiPoliciesController.prototype.getDropzoneClass = function (path) {
        return "gravitee-policy-dropzone " +
            'gravitee-policy-dropzone-filled' +
            " dropzone-" + _.kebabCase(path);
    };
    ApiPoliciesController.prototype.toggleHttpMethod = function (method, methods) {
        var index = methods.indexOf(method);
        if (index > -1) {
            methods.splice(index, 1);
        }
        else {
            methods.push(method);
        }
    };
    ApiPoliciesController.prototype.filterByMethod = function (policy) {
        var _this = this;
        return _.reduce(_.map(policy.methods, function (method) {
            return _this.httpMethodsFilter.indexOf(method) < 0;
        }), function (result, n) { return result && n; });
    };
    ApiPoliciesController.prototype.removePolicy = function (index, path, ev) {
        ev.stopPropagation();
        this.selectedApiPolicy = null;
        var hashKey = this.apiPoliciesByPath[path][index].$$hashKey;
        var that = this;
        this.$mdDialog.show({
            controller: 'DialogConfirmController',
            controllerAs: 'ctrl',
            template: require('../../../components/dialog/confirmWarning.dialog.html'),
            clickOutsideToClose: true,
            locals: {
                title: 'Are you sure you want to remove this policy ?',
                confirmButton: 'Remove'
            }
        }).then(function (response) {
            if (response) {
                _.forEach(that.apiPoliciesByPath[path], function (policy, idx) {
                    if (policy.$$hashKey === hashKey) {
                        that.apiPoliciesByPath[path].splice(idx, 1);
                        return false;
                    }
                });
                that.savePaths();
            }
        });
    };
    ApiPoliciesController.prototype.editPolicyDescription = function (index, path, ev) {
        ev.stopPropagation();
        this.selectedApiPolicy = null;
        var policy = this.apiPoliciesByPath[path][index];
        var that = this;
        this.$mdDialog.show({
            controller: 'DialogEditPolicyController',
            controllerAs: 'editPolicyDialogCtrl',
            template: require('./dialog/policy.dialog.html'),
            clickOutsideToClose: true,
            locals: {
                description: policy.description
            }
        }).then(function (description) {
            policy.description = description;
            that.savePaths();
        }, function () {
            // You cancelled the dialog
        });
    };
    ApiPoliciesController.prototype.switchPolicyEnabled = function (index, path, ev) {
        ev.stopPropagation();
        this.selectedApiPolicy = null;
        var policy = this.apiPoliciesByPath[path][index];
        policy.enabled = !policy.enabled;
        this.savePaths();
    };
    ApiPoliciesController.prototype.savePaths = function () {
        var _this = this;
        this.$scope.$parent.apiCtrl.api.paths = _.cloneDeep(this.apiPoliciesByPath);
        _.forEach(this.$scope.$parent.apiCtrl.api.paths, function (policies) {
            _.forEach(policies, function (policy) {
                delete policy.policyId;
                delete policy.name;
                delete policy.type;
                delete policy.version;
                delete policy.schema;
                // do not save empty fields on arrays
                _.forOwn(policy, function (policyAttributeValueObject) {
                    _.forOwn(policyAttributeValueObject, function (policyAttributeAttribute) {
                        if (_.isArray(policyAttributeAttribute)) {
                            _.remove(policyAttributeAttribute, function (policyAttributeAttributeItem) {
                                return policyAttributeAttributeItem === undefined || '' === policyAttributeAttributeItem;
                            });
                        }
                    });
                });
            });
        });
        return this.ApiService.update(this.$scope.$parent.apiCtrl.api).then(function (_a) {
            var data = _a.data;
            _this.$scope.$parent.apiCtrl.api = data;
            _this.$rootScope.$broadcast('apiChangeSuccess');
            _this.NotificationService.show('API \'' + _this.$scope.$parent.apiCtrl.api.name + '\' saved');
            _this.pathsToCompare = _this.generatePathsToCompare();
        });
    };
    ApiPoliciesController.prototype.showAddPathModal = function (event) {
        var _this = this;
        this.$mdDialog.show({
            controller: 'AddPoliciesPathController',
            controllerAs: 'addPoliciesPathCtrl',
            template: require('./addPoliciesPath.html'),
            parent: angular.element(document.body),
            targetEvent: event,
            clickOutsideToClose: true,
            locals: {
                paths: this.apiPoliciesByPath,
                rootCtrl: this
            }
        }).then(function (paths) {
            _this.apiPoliciesByPath = paths;
            _this.savePaths();
        });
    };
    ApiPoliciesController.prototype.removePath = function (path) {
        this.selectedApiPolicy = {};
        var that = this;
        this.$mdDialog.show({
            controller: 'DialogConfirmController',
            controllerAs: 'ctrl',
            template: require('../../../components/dialog/confirmWarning.dialog.html'),
            clickOutsideToClose: true,
            locals: {
                title: 'Are you sure you want to remove this path ?',
                confirmButton: 'Remove'
            }
        }).then(function (response) {
            if (response) {
                delete that.apiPoliciesByPath[path];
                that.savePaths();
            }
        });
    };
    ApiPoliciesController.prototype.pathNotExists = function (path, index) {
        if (!path || path.trim() === "") {
            return true;
        }
        if (index && this.clearPathParam(path) === this.clearPathParam(this.sortedPaths()[index])) {
            return true;
        }
        return !_.includes(this.pathsToCompare, this.clearPathParam(path));
    };
    ApiPoliciesController.prototype.pathStartWithSlash = function (path) {
        if (!path || path.trim() === "") {
            return true;
        }
        return path[0] === "/";
    };
    ApiPoliciesController.prototype.clearPathParam = function (path) {
        if (path === "/") {
            return "/";
        }
        else {
            return path.trim().replace(/(:.*?\/)|(:.*$)/g, ":x\/").replace(/\/+$/, "");
        }
    };
    ApiPoliciesController.prototype.sortedPaths = function () {
        var _this = this;
        var paths = _.keys(this.apiPoliciesByPath);
        return _.sortBy(paths, function (path) {
            return _this.clearPathParam(path);
        });
    };
    ApiPoliciesController.prototype.pathKeyPress = function (ev, el, newPath, index) {
        switch (ev.keyCode) {
            case 13:
                if (!el.$invalid) {
                    var oldPath = this.sortedPaths()[index];
                    this.apiPoliciesByPath[newPath] = this.apiPoliciesByPath[oldPath];
                    delete this.apiPoliciesByPath[oldPath];
                    this.savePaths();
                }
                break;
            case 27:
                this.restoreOldPath(index, el);
                break;
            default:
                break;
        }
    };
    ApiPoliciesController.prototype.restoreOldPath = function (index, el) {
        el.$setViewValue(this.sortedPaths()[index]);
        el.$commitViewValue();
        // TODO: check editPathForm on form
        document.forms.editPathForm['path' + index].value = this.sortedPaths()[index];
    };
    ApiPoliciesController.prototype.hasProperties = function (apiPolicy) {
        return _.keys(apiPolicy).length;
    };
    return ApiPoliciesController;
}());
exports.default = ApiPoliciesController;
