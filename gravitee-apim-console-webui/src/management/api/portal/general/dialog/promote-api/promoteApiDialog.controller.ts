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

import { IOnInit } from 'angular';
import '@gravitee/ui-components/wc/gv-select';
import * as angular from 'angular';

import { PromotionService } from '../../../../../../services/promotion.service';
import NotificationService from '../../../../../../services/notification.service';

interface PromotionTargetVM {
  id: string;
  name: string;
  promotionInProgress: boolean;
}

export class PromoteApiDialogController implements IOnInit {
  public promotionTargets: PromotionTargetVM[];
  public selectedPromotionTarget?: PromotionTargetVM;
  public isLoading = false;
  public hasPromotionInProgress = false;
  private cockpitURL: string;
  private hasCockpit: boolean;

  constructor(
    private readonly promotionService: PromotionService,
    private readonly NotificationService: NotificationService,
    private readonly $mdDialog: angular.material.IDialogService,
    private readonly api: {
      id: string;
      name: string;
    },
  ) {
    'ngInject';
  }

  $onInit(): void {
    this.isLoading = true;
    this.hasCockpit = true;

    Promise.all([
      this.promotionService.listPromotionTargets(),
      this.promotionService.listPromotion({
        apiId: this.api.id,
        statuses: ['CREATED', 'TO_BE_VALIDATED'],
      }),
    ])
      .then(([targetEnvs, promotions]) => {
        this.promotionTargets = targetEnvs
          .map((promotionTarget) => ({
            name: promotionTarget.name,
            id: promotionTarget.id,
            promotionInProgress: promotions.some((promotion) => promotion.targetEnvCockpitId === promotionTarget.id),
          }))
          .sort((target1, target2) => target1.name.localeCompare(target2.name));

        this.hasPromotionInProgress = this.promotionTargets.some((target) => target.promotionInProgress);
        const selectableTarget = this.promotionTargets.filter((target) => !target.promotionInProgress);
        if (selectableTarget.length === 1) {
          this.selectedPromotionTarget = selectableTarget[0];
        }
      })
      .catch((err) => {
        if (err && err.data.technicalCode === 'installation.notAccepted') {
          err.interceptorFuture.cancel();
          this.hasCockpit = false;
          const { cockpitURL } = err.data.parameters;
          this.cockpitURL = cockpitURL;
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
