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

import { QualityRule } from '../entities/QualityRule';
import { IHttpPromise } from 'angular';
import { ApiQualityRule } from '../entities/apiQualityRule';

class QualityRuleService {
  private URL: string;
  private URL_API: string;

  constructor(private $http, Constants) {
    'ngInject';
    this.URL = `${Constants.envBaseURL}/configuration/quality-rules/`;
    this.URL_API = `${Constants.envBaseURL}/apis/`;
  }

  list(): IHttpPromise<QualityRule[]> {
    return this.$http.get(this.URL);
  }

  get(id: string): IHttpPromise<QualityRule[]> {
    return this.$http.get(this.URL + id);
  }

  create(qualityRule: QualityRule): IHttpPromise<QualityRule> {
    return this.$http.post(this.URL, qualityRule);
  }

  update(qualityRule: QualityRule): IHttpPromise<QualityRule> {
    return this.$http.put(this.URL + qualityRule.id,
      {
        name: qualityRule.name,
        description: qualityRule.description,
        weight: qualityRule.weight
      });
  }

  delete(qualityRule: QualityRule): IHttpPromise<any> {
    return this.$http.delete(this.URL + qualityRule.id);
  }

  listByApi(api: string): IHttpPromise<ApiQualityRule[]> {
    return this.$http.get(this.URL_API + api + '/quality-rules/');
  }

  createApiRule(api: string, qualityRule: string, checked: boolean) {
    return this.$http.post(this.URL_API + api + '/quality-rules/', {
      api: api,
      quality_rule: qualityRule,
      checked: checked
    });
  }

  updateApiRule(api: string, qualityRule: string, checked: boolean) {
    return this.$http.put(this.URL_API + api + '/quality-rules/' + qualityRule, {
      checked: checked
    });
  }
}

export default QualityRuleService;
