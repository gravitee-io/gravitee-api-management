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
import TagService from '../../../services/tag.service';
import NotificationService from '../../../services/notification.service';
import PortalSettingsService from '../../../services/portalSettings.service';
import { IPromise, IScope } from 'angular';
import '@gravitee/ui-components/wc/gv-policy-studio';
import OrganizationService, { Organization } from '../../../services/organization.service';
import { response } from 'express';

class PoliciesController {
  private tags: any;
  private organization: Organization;
  private definition: any;

  constructor(
    private TagService: TagService,
    private NotificationService: NotificationService,
    private $q: ng.IQService,
    private PortalSettingsService: PortalSettingsService,
    private $rootScope: IScope,
    private OrganizationService: OrganizationService,
    private readonly $mdDialog: angular.material.IDialogService,
  ) {
    'ngInject';
    this.$rootScope = $rootScope;
  }

  $onInit = () => {
    this.OrganizationService.get().then(({ data: { id, ...organization } }) => {
      this.setOrganization(organization);
    });
  };

  fetchTags = ({ detail }) => {
    detail.currentTarget.options = this.tags.map((t) => ({ value: t.id, label: t.name }));
  };

  setOrganization(organization) {
    if (organization !== this.organization) {
      this.organization = organization;
      this.definition = {
        flows:
          this.organization.flows != null
            ? this.organization.flows.map((flow) => {
                flow.consumers = flow.consumers.map((consumer) => consumer.consumerId);
                return flow;
              })
            : [],
        flow_mode: this.organization.flowMode,
      };
    }
  }

  onSave(event) {
    const { definition } = event.detail;
    this.organization.flows = definition.flows.map((flow) => {
      flow.consumers = (flow.consumers || []).map((consumer) => {
        return {
          consumerType: 'TAG',
          consumerId: consumer,
        };
      });
      return flow;
    });
    this.organization.flowMode = definition.flow_mode;

    this.showConfirmDialog().then((validation) => {
      if (validation) {
        this.OrganizationService.update(this.organization).then(() => {
          this.NotificationService.show('Platform policies has been updated');
          event.target.saved();
        });
      }
    });
  }

  private showConfirmDialog(): IPromise<boolean> {
    return this.$mdDialog.show({
      controller: 'DialogConfirmController',
      controllerAs: 'ctrl',
      template: require('../../../components/dialog/confirm.dialog.html'),
      clickOutsideToClose: true,
      locals: {
        title: 'Deploy the policies?',
        msg: 'Platform policies will be automatically deployed on gateways.',
      },
    });
  }
}

export default PoliciesController;
