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
import * as _ from 'lodash';
import * as angular from 'angular';

import ApplicationService from '../../../../services/applications.service';
import NotificationService from '../../../../services/notification.service';
import ApiService from '../../../../services/api.service';
import { PagedResult } from "../../../../entities/pagedResult";

export class SubscriptionQuery {
  status?: string[] = ['ACCEPTED', 'PENDING'];
  apis?: string[];
  page?: number = 1;
  size?: number = 20;
}

class ApplicationSubscriptionsController {

  private subscriptions: PagedResult;
  private application: any;

  private query: SubscriptionQuery = new SubscriptionQuery();

  private status = {
    'ACCEPTED': 'Accepted',
    'CLOSED': 'Closed',
    'PENDING': 'Pending',
    'REJECTED': 'Rejected'
  };

  private subscriptionsFiltersForm: any;

  constructor(
    private ApplicationService: ApplicationService,
    private NotificationService: NotificationService,
    private $mdDialog: angular.material.IDialogService,
    private ApiService: ApiService
  ) {
    'ngInject';

    this.onPaginate = this.onPaginate.bind(this);
  }

  onPaginate(page) {
    this.query.page = page;
    this.doSearch();
  }

  clearFilters() {
    this.subscriptionsFiltersForm.$setPristine();
    this.query = new SubscriptionQuery();
    this.doSearch();
  }

  search() {
    this.query.page = 1;
    this.query.size = 20;
    this.doSearch();
  }

  buildQuery() {
    let query = '?page=' + this.query.page + '&size=' + this.query.size + '&';
    let parameters = {};

    if (this.query.status !== undefined) {
      parameters['status'] = this.query.status.join(',');
    }

    if (this.query.apis !== undefined) {
      parameters['api'] = this.query.apis.join(',');
    }

    _.mapKeys(parameters, (value, key ) => {
      return query += key + '=' + value + '&';
    });

    return query;
  }

  doSearch() {
    let query = this.buildQuery();

    this.ApplicationService.listSubscriptions(this.application.id, query).then((response) => {
      this.subscriptions = response.data as PagedResult;
    });
  }

  toggleSubscription(scope, subscription) {
    scope.toggle();
    if (!subscription.apiKeys) {
      this.listApiKeys(subscription);
    }
  }

  listApiKeys(subscription) {
    this.ApplicationService.listApiKeys(this.application.id, subscription.id).then(response => {
      subscription.apiKeys = response.data;
    });
  }

  hasKeysDefined() {
    return this.subscriptions !== null && Object.keys(this.subscriptions).length > 0;
  }

  generateAPIKey(applicationId, subscription) {
    var _this = this;
    this.$mdDialog.show({
      controller: 'DialogConfirmController',
      controllerAs: 'ctrl',
      template: require('../../../../components/dialog/confirmWarning.dialog.html'),
      clickOutsideToClose: true,
      locals: {
        title: 'Are you sure you want to renew your API Key ?',
        msg: 'Your previous API Key will be no longer valid in 1 hour !',
        confirmButton: 'Renew'
      }
    }).then(function (response) {
      if (response) {
        _this.ApplicationService.renewApiKey(applicationId, subscription.id).then(() => {
          _this.NotificationService.show('A new API Key has been generated');
          _this.listApiKeys(subscription);
        });
      }
    });
  }

  revoke(subscription, apiKey) {
    var _this = this;
    this.$mdDialog.show({
      controller: 'DialogConfirmController',
      controllerAs: 'ctrl',
      template: require('../../../../components/dialog/confirmWarning.dialog.html'),
      clickOutsideToClose: true,
      locals: {
        title: 'Are you sure you want to revoke API Key \'' + apiKey + '\'?',
        confirmButton: 'Revoke'
      }
    }).then(function (response) {
      if (response) {
        _this.ApplicationService.revokeApiKey(_this.application.id, subscription.id, apiKey).then(() => {
          _this.NotificationService.show('API Key ' + apiKey + ' has been revoked !');
          _this.listApiKeys(subscription);
        });
      }
    });
  }

  onClipboardSuccess(e) {
    this.NotificationService.show('API Key has been copied to clipboard');
    e.clearSelection();
  }

  /*
  showSubscribeApiModal(ev) {
    this.$mdDialog.show({
      controller: 'DialogSubscribeApiController',
      templateUrl: 'application/dialog/subscribeApi.dialog.html',
      parent: angular.element(document.body),
      targetEvent: ev,
      clickOutsideToClose: true,
      locals: {
        application: this.application,
        subscriptions: this.subscriptions
      }
    }).then(application =>{
      if (application) {
        // TODO : check it ! There was no ApiService...
        this.ApiService.getSubscriptions(application.id);
      }
    });
  }
  */

  showExpirationModal(apiId, apiKey) {
    this.$mdDialog.show({
      controller: 'DialogApiKeyExpirationController',
      controllerAs: 'dialogApiKeyExpirationController',
      template: require('../../../api/portal/subscriptions/apikey.expiration.dialog.html'),
      clickOutsideToClose: true
    }).then(expirationDate =>{
      apiKey.expire_at = expirationDate;

      this.ApiService.updateApiKey(apiId, apiKey).then(() => {
        this.NotificationService.show('An expiration date has been settled for API Key');
      });
    });
  }
}

export default ApplicationSubscriptionsController;
