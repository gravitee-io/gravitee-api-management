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
import * as moment from 'moment';

function DialogApiKeyExpirationController($scope, $mdDialog) {
  'ngInject';

  $scope.minutes = ['00', '01', '02', '03', '04', '05', '06', '07', '08', '09', '10', '11', '12', '13', '14', '15',
    '16', '17', '18', '19', '20', '21', '22', '23', '24', '25', '26', '27', '28', '29', '30', '31', '32', '33', '34',
    '35', '36', '37', '38', '39', '40', '41', '42', '43', '44', '45', '46', '47', '48', '49', '50', '51', '52', '53',
    '54', '55', '56', '57', '58', '59'];

  $scope.hours = ['00', '01', '02', '03', '04', '05', '06', '07', '08', '09', '10', '11', '12', '13', '14', '15',
    '16', '17', '18', '19', '20', '21', '22', '23'];

  $scope.now = new Date();
  $scope.minDate = new Date(
    $scope.now.getFullYear(),
    $scope.now.getMonth(),
    $scope.now.getDate());

  $scope.expiration = {
    date: $scope.now,
    time: {
      hours: moment($scope.now).hours().toString(),
      minutes: moment($scope.now).minutes().toString()
    }
  };

  this.hide = function () {
    $mdDialog.cancel();
  };

  this.save = function () {
    var m = moment($scope.expiration.date);
    m.hours($scope.expiration.time.hours);
    m.minutes($scope.expiration.time.minutes);
    m.seconds(0);

    $mdDialog.hide(m.valueOf());
  };
}

export default DialogApiKeyExpirationController;
