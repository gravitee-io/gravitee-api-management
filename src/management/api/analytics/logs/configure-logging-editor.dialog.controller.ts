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

import _ = require('lodash');
import moment = require('moment');

export class ConditionType {
  title: string;
  id: string;
  statement: string;

  constructor(title, id, statement) {
    this.title = title;
    this.id = id;
    this.statement = statement;
  }

}

export class Condition {
  type: ConditionType;
  operator: string;
  value: string;
  param1?: any;
  param2?: any;

  constructor(type: ConditionType, operator: string, value: string) {
    this.type = type;
    this.operator = operator;
    this.value = value;
  }

  toCondition() {
    if (this.type.id === 'logging-duration') {
      let end = moment().add(moment.duration(this.param1, this.param2));
      return `${this.type.statement} <= ${end}l`;
    } if (this.type.id === 'logging-end-date') {
      let end = this.param1;
      return `${this.type.statement} <= ${end}l`;
    } else if (this.type.id !== 'request-header' && this.type.id !== 'request-param') {
      return `${this.type.statement} ${this.operator} '${this.value}'`;
    } else {
      return `${this.type.statement}['${this.param1}'] != null && ${this.type.statement}['${this.param1}'][0] ${this.operator} '${this.value}'`;
    }
  }
}

function DialogConfigureLoggingEditorController($scope, $mdDialog, plans, subscribers) {
  'ngInject';

  $scope.selectedType = null;

  $scope.plans = plans;
  $scope.subscribers = subscribers;
  $scope.methods = ['GET', 'POST', 'PUT', 'DELETE', 'PATCH', 'HEAD', 'OPTIONS'];
  $scope.timeUnits = ['SECONDS', 'MINUTES', 'HOURS', 'DAYS'];
  $scope.types = [ConditionType];
  $scope.conditions = [];

  $scope.types.push(new ConditionType('Plan', 'plan', '#context.plan'));
  $scope.types.push(new ConditionType('Application', 'application', '#context.application'));
  $scope.types.push(new ConditionType('Request header', 'request-header', '#request.headers'));
  $scope.types.push(new ConditionType('Request query-parameter', 'request-param', '#request.params'));
  $scope.types.push(new ConditionType('HTTP Method', 'request-method', '#request.method'));
  $scope.types.push(new ConditionType('Request IP', 'request-remote-address', '#request.remoteAddress'));
  $scope.types.push(new ConditionType('Duration', 'logging-duration', '#request.timestamp'));
  $scope.types.push(new ConditionType('End date', 'logging-end-date', '#request.timestamp'));

  this.addCondition = () => {
    let type: ConditionType = _.find($scope.types, { 'id': $scope.selectedType});
    if (type !== undefined) {
      $scope.conditions.push(new Condition(type, '==', ''));
    }
    $scope.selectedType = null;
  };

  this.removeCondition = (idx: number) => {
    $scope.conditions.splice(idx, 1);
  };

  this.hide = () => {
    $mdDialog.hide();
  };

  this.save = () => {
    let condition = _($scope.conditions).map((condition) => condition.toCondition()).join(' && ');

    $mdDialog.hide(condition);
  };
}

export default DialogConfigureLoggingEditorController;
