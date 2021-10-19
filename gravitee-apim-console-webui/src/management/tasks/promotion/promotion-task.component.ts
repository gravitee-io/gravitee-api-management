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
import { IPromise } from 'angular';

import { PromotionTask } from '../../../entities/task/task';
import { PromotionService } from '../../../services/promotion.service';

class PromotionTaskComponentController {
  public task: PromotionTask;
  public taskHandled: () => void;

  constructor(private readonly $mdDialog: angular.material.IDialogService, private readonly promotionService: PromotionService) {
    'ngInject';
  }

  public openRejectDialog(): IPromise<void> {
    return this.$mdDialog
      .show({
        controller: 'DialogConfirmController',
        controllerAs: 'ctrl',
        template: require('../../../components/dialog/confirmWarning.dialog.html'),
        clickOutsideToClose: true,
        locals: {
          title: 'Reject Promotion Request',
          msg: 'After having rejected this promotion you will not be able to accept it without asking the author to create a new promotion',
        },
      })
      .then((dialogValidation) => {
        if (dialogValidation) {
          this.promotionService.processPromotion(this.task.data.promotionId, false).then(() => this.taskHandled());
        }
      });
  }

  public openAcceptDialog(): IPromise<void> {
    const msg = this.task.data.isApiUpdate
      ? `Since the API <code>${this.task.data.apiName}</code> has already been promoted to <strong>${this.task.data.targetEnvironmentName}</strong> environment, accepting this promotion will update it.`
      : `Accepting this promotion will create a new API in <strong>${this.task.data.targetEnvironmentName}</strong> environment.`;
    return this.$mdDialog
      .show({
        controller: 'DialogConfirmController',
        controllerAs: 'ctrl',
        template: require('../../../components/dialog/confirm.dialog.html'),
        clickOutsideToClose: true,
        locals: {
          title: 'Accept Promotion Request',
          msg,
        },
      })
      .then((dialogValidation) => {
        if (dialogValidation) {
          this.promotionService.processPromotion(this.task.data.promotionId, true).then(() => this.taskHandled());
        }
      });
  }
}

export const PromotionTaskComponent: ng.IComponentOptions = {
  template: require('./promotion-task.html'),
  bindings: {
    task: '<',
    taskHandled: '&',
  },
  controller: PromotionTaskComponentController,
};
