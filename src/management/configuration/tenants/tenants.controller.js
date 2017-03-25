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
var TenantsController = (function () {
    function TenantsController(TenantService, NotificationService, $q, $mdEditDialog, $mdDialog) {
        'ngInject';
        this.TenantService = TenantService;
        this.NotificationService = NotificationService;
        this.$q = $q;
        this.$mdEditDialog = $mdEditDialog;
        this.$mdDialog = $mdDialog;
        this.tenantsToCreate = [];
        this.tenantsToUpdate = [];
    }
    TenantsController.prototype.newTenant = function (event) {
        var _this = this;
        event.stopPropagation();
        var that = this;
        var promise = this.$mdEditDialog.small({
            placeholder: 'Add a name',
            save: function (input) {
                var tenant = { name: input.$modelValue };
                _this.tenants.push(tenant);
                _this.tenantsToCreate.push(tenant);
            },
            targetEvent: event,
            validators: {
                'md-maxlength': 30
            }
        });
        promise.then(function (ctrl) {
            var input = ctrl.getInput();
            input.$viewChangeListeners.push(function () {
                input.$setValidity('empty', input.$modelValue.length !== 0);
                input.$setValidity('duplicate', !_.includes(_.map(that.tenants, 'name'), input.$modelValue));
            });
        });
    };
    TenantsController.prototype.editName = function (event, tenant) {
        event.stopPropagation();
        var that = this;
        var promise = this.$mdEditDialog.small({
            modelValue: tenant.name,
            placeholder: 'Add a name',
            save: function (input) {
                tenant.name = input.$modelValue;
                if (!_.includes(that.tenantsToCreate, tenant)) {
                    that.tenantsToUpdate.push(tenant);
                }
            },
            targetEvent: event,
            validators: {
                'md-maxlength': 30
            }
        });
        promise.then(function (ctrl) {
            var input = ctrl.getInput();
            input.$viewChangeListeners.push(function () {
                input.$setValidity('empty', input.$modelValue.length !== 0);
            });
        });
    };
    TenantsController.prototype.editDescription = function (event, tenant) {
        event.stopPropagation();
        var that = this;
        this.$mdEditDialog.small({
            modelValue: tenant.description,
            placeholder: 'Add a description',
            save: function (input) {
                tenant.description = input.$modelValue;
                if (!_.includes(that.tenantsToCreate, tenant)) {
                    that.tenantsToUpdate.push(tenant);
                }
            },
            targetEvent: event,
            validators: {
                'md-maxlength': 160
            }
        });
    };
    TenantsController.prototype.saveTenants = function () {
        var that = this;
        this.$q.all([
            this.TenantService.create(that.tenantsToCreate),
            this.TenantService.update(that.tenantsToUpdate)
        ]).then(function () {
            that.NotificationService.show("Tenants saved with success");
            //      that.loadTenants();
            that.tenantsToCreate = [];
            that.tenantsToUpdate = [];
        });
    };
    TenantsController.prototype.deleteTenant = function (tenant) {
        var that = this;
        this.$mdDialog.show({
            controller: 'DeleteTenantDialogController',
            template: require('./delete.tenant.dialog.html'),
            locals: {
                tenant: tenant
            }
        }).then(function (deleteTenant) {
            if (deleteTenant) {
                if (tenant.id) {
                    that.TenantService.delete(tenant).then(function () {
                        that.NotificationService.show("Tenant '" + tenant.name + "' deleted with success");
                        _.remove(that.tenants, tenant);
                    });
                }
                else {
                    _.remove(that.tenantsToCreate, tenant);
                    _.remove(that.tenants, tenant);
                }
            }
        });
    };
    TenantsController.prototype.reset = function () {
        this.tenants = _.cloneDeep(this.initialTenants);
        this.tenantsToCreate = [];
        this.tenantsToUpdate = [];
    };
    return TenantsController;
}());
exports.default = TenantsController;
