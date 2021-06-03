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
import { PromotionService } from '../../../../../../services/promotion.service';
import { PromotionTarget } from '../../../../../../entities/promotion';
import { IOnInit } from 'angular';
import '@gravitee/ui-components/wc/gv-select';
import * as angular from 'angular';
import NotificationService from '../../../../../../services/notification.service';

export class PromoteApiDialogController implements IOnInit {
  public promotionTargets: PromotionTarget[] = [];
  public envOptions: Array<{ label: string; value: string }>;
  public isLoading = false;
  public selectedPromotionTarget?: PromotionTarget;

  constructor(
    private readonly promotionService: PromotionService,
    private readonly NotificationService: NotificationService,
    private readonly $mdDialog: angular.material.IDialogService,
    private readonly api: any,
  ) {
    'ngInject';
  }

  $onInit(): void {
    this.isLoading = true;
    // should we do this in the popup or in the parent page (which means cockpit requests for nothing maybe)
    this.promotionService
      .listPromotionTargets()
      .then((environments) => {
        this.promotionTargets = environments;
        this.envOptions = environments.map((environment) => ({ label: environment.name, value: environment.id }));

        if (environments.length === 1) {
          this.selectedPromotionTarget = environments[0];
        }
      })
      .finally(() => {
        this.isLoading = false;
      });
  }

  promote() {
    this.promotionService
      .promote(this.api.id, this.selectedPromotionTarget)
      .then(() => {
        this.NotificationService.show(`Promotion requested for api ${this.api.name}`);
        this.hide();
      })
      .catch(() => {
        this.NotificationService.showError(`A problem occurs when trying to promote api ${this.api.name}`);
      });
  }

  hide() {
    this.$mdDialog.hide();
  }
}
