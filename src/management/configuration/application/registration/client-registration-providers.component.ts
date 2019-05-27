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
import NotificationService from "../../../../services/notification.service";
import PortalConfigService from "../../../../services/portalConfig.service";
import { ClientRegistrationProvider } from "../../../../entities/clientRegistrationProvider";
import ClientRegistrationProviderService from "../../../../services/clientRegistrationProvider.service";

const ClientRegistrationProvidersComponent: ng.IComponentOptions = {
  bindings: {
    clientRegistrationProviders: '<'
  },
  template: require('./client-registration-providers.html'),
  controller: function(
    $mdDialog: angular.material.IDialogService,
    ClientRegistrationProviderService: ClientRegistrationProviderService,
    PortalConfigService: PortalConfigService,
    NotificationService: NotificationService,
    $state: StateService,
    Constants
  ) {
    'ngInject';

    this.settings = _.cloneDeep(Constants);

    this.select = (provider: ClientRegistrationProvider) => {
      $state.go('management.settings.clientregistrationproviders.clientregistrationprovider', {id: provider.id});
    };

    this.create = (type) => {
      $state.go('management.settings.clientregistrationproviders.create', {type: type});
    };

    this.delete = (provider: ClientRegistrationProvider) => {
      $mdDialog.show({
        controller: 'DialogConfirmController',
        controllerAs: 'ctrl',
        template: require('../../../../components/dialog/confirmWarning.dialog.html'),
        clickOutsideToClose: true,
        locals: {
          title: 'Are you sure you want to delete this client registration provider ?',
          msg: '',
          confirmButton: 'Delete'
        }
      }).then(function (response) {
        if (response) {
          ClientRegistrationProviderService.delete(provider).then(response => {
            NotificationService.show("Client registration provider '" + provider.name + "' has been deleted");
            $state.go('management.settings.clientregistrationproviders.list', {}, {reload: true});
          });
        }
      });
    };

    this.saveClientRegistration = () => {
      PortalConfigService.save({
        application: {
          registration: {
            enabled: this.settings.application.registration.enabled
          }
        }
      }).then( response => {
        NotificationService.show("Client registration is now " + (this.settings.application.registration.enabled?"mandatory":"optional") );
        _.merge(Constants, response.data);
      });
    };

    this.saveApplicationType = (type: string) => {
      let appType = {
        application: {
          types: {
            [type]: {
              enabled: this.settings.application.types[type].enabled
            }
          }
        }
      };

      appType['application']['types'][type] = {
        enabled: this.settings.application.types[type].enabled
      };

      PortalConfigService.save(appType).then( response => {
        NotificationService.show("Application type '" + type  + "' is now " + (this.settings.application.types[type].enabled?"allowed":"disallowed") );
        _.merge(Constants, response.data);
      });
    };
  }
};

export default ClientRegistrationProvidersComponent;
