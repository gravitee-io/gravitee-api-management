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
class TenantsController {
  constructor($scope, TenantService, NotificationService, $q, $mdEditDialog, $mdDialog) {
    'ngInject';

    this.$scope = $scope;
    this.TenantService = TenantService;
    this.NotificationService = NotificationService;
    this.$q = $q;
    this.$mdEditDialog = $mdEditDialog;
    this.$mdDialog = $mdDialog;

    this.loadTenants();
    this.tenantsToCreate = [];
    this.tenantsToUpdate = [];
  }

  loadTenants() {
    var that = this;
    this.TenantService.list().then(function (response) {
      that.tenants = response.data;
      _.each(that.tenants, function(tenant) {
        delete tenant.totalApis;
      });
      that.initialTenants = _.cloneDeep(that.tenants);
    });
  }

  newTenant(event) {
    event.stopPropagation();

    var that = this;

    var promise = this.$mdEditDialog.small({
      placeholder: 'Add a name',
      save: function (input) {
        var tenant = {name: input.$modelValue};
        that.tenants.push(tenant);
        that.tenantsToCreate.push(tenant);
      },
      targetEvent: event,
      validators: {
        'md-maxlength': 30
      }
    });

    promise.then(function (ctrl) {
      var input = ctrl.getInput();

      input.$tenantChangeListeners.push(function () {
        input.$setValidity('empty', input.$modelValue.length !== 0);
        input.$setValidity('duplicate', !_.includes(_.map(that.tenants, 'name'), input.$modelValue));
      });
    });
  }

  editName(event, tenant) {
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

      input.$tenantChangeListeners.push(function () {
        input.$setValidity('empty', input.$modelValue.length !== 0);
      });
    });
  }

  editDescription(event, tenant) {
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
  }

  saveTenants() {
    var that = this;

    this.$q.all([
      this.TenantService.create(that.tenantsToCreate),
      this.TenantService.update(that.tenantsToUpdate)
    ]).then(function () {
      that.NotificationService.show("Tenants saved with success");
      that.loadTenants();
      that.tenantsToCreate = [];
      that.tenantsToUpdate = [];
    });
  }

  deleteTenant(tenant) {
    var that = this;
    this.$mdDialog.show({
      controller: 'DeleteTenantDialogController',
      templateUrl: 'app/configuration/admin/tenants/delete.tenant.dialog.html',
      tenant: tenant
    }).then(function (deleteTenant) {
      if (deleteTenant) {
        if (tenant.id) {
          that.TenantService.delete(tenant).then(function () {
            that.NotificationService.show("Tenant '" + tenant.name + "' deleted with success");
            _.remove(that.tenants, tenant);
          });
        } else {
          _.remove(that.tenantsToCreate, tenant);
          _.remove(that.tenants, tenant);
        }
      }
    });
  }

  reset() {
    this.tenants = _.cloneDeep(this.initialTenants);
    this.tenantsToCreate = [];
    this.tenantsToUpdate = [];
  }
}

export default TenantsController;
