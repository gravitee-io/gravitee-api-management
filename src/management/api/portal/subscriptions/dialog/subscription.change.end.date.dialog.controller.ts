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
function DialogSubscriptionChangeEndDateController($scope, $mdDialog, subscription) {
  'ngInject';
  this.subscription = subscription;
  this.endDate = subscription.endingAt;
  this.now = new Date();

  this.hide = function () {
    $mdDialog.cancel();
  };

  this.save = function () {
    $mdDialog.hide(this.endDate ? this.endDate.toDate() : null);
  };
}

export default DialogSubscriptionChangeEndDateController;
