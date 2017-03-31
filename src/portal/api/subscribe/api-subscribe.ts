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

function ApiSubscribeController($state,
                                NotificationService,
                                ApplicationService,
                                ApiService,
                                $translate,
                                $location) {
  'ngInject';
  const vm = this;

  this.$onInit = function () {
    if (vm.plan.paths['/'] && vm.plan.paths['/'].length) {
      _.forEach(vm.plan.paths['/'], function (path) {
        if (path.quota) {
          let quota = path.quota.quota;
          $translate('common.' + quota.periodTimeUnit).then(function (quotaPeriodTimeUnitTranslated) {
            $translate('api.subscription.planInformation',
              {
                quotaLimit: quota.limit,
                quotaPeriodTime: quota.periodTime,
                quotaPeriodTimeUnit: quotaPeriodTimeUnitTranslated
              })
              .then(function (translatedMessage) {
                vm.planInformation = translatedMessage;
              });
          });
        }
      });
    }
  };

  function getSubscription(application) {
    if (!application) {
      return;
    }
    const subscriptionsByApplication = _.groupBy(vm.subscriptions, 'application');
    return _.find(subscriptionsByApplication[application.id], function (subscriptionByApplication: any) {
      return _.includes(['accepted', 'pending'], subscriptionByApplication.status);
    });
  }

  vm.isNotSelectable = function (application) {
    if (!application) {
      return true;
    }
    return getSubscription(application);
  };

  function fetchApiKey(application) {
    vm.subscription = getSubscription(application);
    if (application && vm.subscription) {
      ApiService.listApiKeys(application.id, vm.subscription.id).then(function (apiKeys) {
        let apiKey = _.find(apiKeys.data, function (apiKey: any) {
          return !apiKey.revoked;
        });
        if (apiKey) {
          vm.apiKey = apiKey.key;
        }
      });
    }
  }

  vm.select = function (application) {
    ApplicationService.subscribe(application.id, vm.plan.id).then(function () {
      NotificationService.show('api.subscription.successful', {planName: vm.plan.name});

      ApiService.getPlanSubscriptions(vm.api.id, vm.plan.id).then(function (subscriptions) {
        vm.subscriptions = subscriptions.data;
        fetchApiKey(application);
      });
    });
  };

  vm.goToApplications = function () {
    $state.go('applications.list', {}, {reload: true});
  };

  vm.subscribable = function () {
    return 'key_less' !== vm.plan.security;
  };

  vm.onClipboardSuccess = function (e) {
    NotificationService.show('api.subscription.apiKeyCopySuccess');
    e.clearSelection();
  };

  vm.onApplicationSearchChange = function () {
    delete vm.apiKey;
  };

  vm.onApplicationSelect = function () {
    fetchApiKey(vm.selectedApp);
  };

  vm.getSampleCall = function () {
    return 'curl -X GET "' + $location.protocol() + '://' + $location.host() + vm.api.proxy.context_path +
      '" -H "Gravitee-ApiKey: ' + (vm.apiKey ? vm.apiKey : 'given_api_key') + '"';
  }
}

export default ApiSubscribeController;
