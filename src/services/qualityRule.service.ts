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

import { IHttpPromise } from 'angular';

import { ApiQualityRule } from '../entities/apiQualityRule';
import { QualityRule } from '../entities/qualityRule';

class QualityRuleService {
  constructor(private $http, private Constants) {
    'ngInject';
  }

  list(): IHttpPromise<QualityRule[]> {
    return this.$http.get(`${this.Constants.env.baseURL}/configuration/quality-rules/`);
  }

  get(id: string): IHttpPromise<QualityRule[]> {
    return this.$http.get(`${this.Constants.env.baseURL}/configuration/quality-rules/` + id);
  }

  create(qualityRule: QualityRule): IHttpPromise<QualityRule> {
    return this.$http.post(`${this.Constants.env.baseURL}/configuration/quality-rules/`, qualityRule);
  }

  update(qualityRule: QualityRule): IHttpPromise<QualityRule> {
    return this.$http.put(`${this.Constants.env.baseURL}/configuration/quality-rules/` + qualityRule.id, {
      name: qualityRule.name,
      description: qualityRule.description,
      weight: qualityRule.weight,
    });
  }

  delete(qualityRule: QualityRule): IHttpPromise<any> {
    return this.$http.delete(`${this.Constants.env.baseURL}/configuration/quality-rules/` + qualityRule.id);
  }

  listByApi(api: string): IHttpPromise<ApiQualityRule[]> {
    return this.$http.get(`${this.Constants.env.baseURL}/apis/` + api + '/quality-rules/');
  }

  createApiRule(api: string, qualityRule: string, checked: boolean) {
    return this.$http.post(`${this.Constants.env.baseURL}/apis/` + api + '/quality-rules/', {
      api: api,
      quality_rule: qualityRule,
      checked: checked,
    });
  }

  updateApiRule(api: string, qualityRule: string, checked: boolean) {
    return this.$http.put(`${this.Constants.env.baseURL}/apis/` + api + '/quality-rules/' + qualityRule, {
      checked: checked,
    });
  }
}

export default QualityRuleService;
