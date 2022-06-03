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
import { IComponentController } from 'angular';

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
      // eslint-disable-next-line import/namespace
      const end: number = moment().add(moment.duration(this.param1, this.param2)).valueOf();
      return `${this.type.statement} <= ${end}l`;
    }
    if (this.type.id === 'logging-end-date') {
      const end: number = (this.param1 as moment.Moment).valueOf();
      return `${this.type.statement} <= ${end}l`;
    } else if (this.type.id !== 'request-header' && this.type.id !== 'request-param') {
      return `${this.type.statement} ${this.operator} '${this.value}'`;
    } else {
      return `${this.type.statement}['${this.param1}'] != null && ${this.type.statement}['${this.param1}'][0] ${this.operator} '${this.value}'`;
    }
  }
}

export class ConfigureLoggingDialogController implements IComponentController {
  public selectedType = null;
  public subscribers;
  public plans;
  public conditions: Condition[] = [];

  public readonly methods = ['GET', 'POST', 'PUT', 'DELETE', 'PATCH', 'HEAD', 'OPTIONS'];
  public readonly timeUnits = ['SECONDS', 'MINUTES', 'HOURS', 'DAYS'];
  public types = [
    new ConditionType('', '', ''),
    new ConditionType('Plan', 'plan', '#context.attributes.plan'),
    new ConditionType('Application', 'application', '#context.attributes.application'),
    new ConditionType('Request header', 'request-header', '#request.headers'),
    new ConditionType('Request query-parameter', 'request-param', '#request.params'),
    new ConditionType('HTTP Method', 'request-method', '#request.method'),
    new ConditionType('Request IP', 'request-remote-address', '#request.remoteAddress'),
    new ConditionType('Duration', 'logging-duration', '#request.timestamp'),
    new ConditionType('End date', 'logging-end-date', '#request.timestamp'),
  ];
  private readonly $mdDialog: angular.material.IDialogService;

  constructor($mdDialog: angular.material.IDialogService, plans, subscribers) {
    'ngInject';
    this.$mdDialog = $mdDialog;
    this.plans = plans;
    this.subscribers = subscribers;
  }

  public addCondition() {
    const type: ConditionType = this.types.find((conditionType: ConditionType) => conditionType.id === this.selectedType);
    if (type !== undefined) {
      this.conditions.push(new Condition(type, '==', ''));
    } else {
      this.selectedType = null;
    }
  }

  public removeCondition(idx: number) {
    this.conditions.splice(idx, 1);
  }

  public hide() {
    this.$mdDialog.hide();
  }

  public save() {
    const condition = this.conditions.map((condition) => condition.toCondition()).join(' && ');

    this.$mdDialog.hide(condition);
  }
}
