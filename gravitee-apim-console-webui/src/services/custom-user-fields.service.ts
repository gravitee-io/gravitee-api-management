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
class CustomUserFieldsService {
  constructor(private $http, private Constants) {
    'ngInject';
  }

  valuesAsList = function (field: any) {
    const transformedField: any = {
      key: field.key,
      label: field.label,
      required: field.required,
    };

    if (field.values) {
      const litsOfValues: string[] = field.values.split('\n');
      transformedField.values = litsOfValues;
    }

    return transformedField;
  };

  mapForUpdateDialog(field: any) {
    const transformedField: any = {
      key: field.key,
      label: field.label,
      required: field.required,
    };

    if (field.values) {
      transformedField.values = field.values.join('\n');
    }

    return transformedField;
  }

  listFormats() {
    // only string for the moment
    // use the same list as metadata.service.ts when additional types will be managed
    return ['string'];
  }

  listPredefinedKeys() {
    return ['organization', 'job_position', 'telephone_number', 'country', 'city', 'zip_code', 'address'].sort();
  }

  list() {
    return this.$http.get(`${this.Constants.org.baseURL}/configuration/custom-user-fields`);
  }

  create(field) {
    return this.$http.post(`${this.Constants.org.baseURL}/configuration/custom-user-fields`, this.valuesAsList(field));
  }

  update(field) {
    return this.$http.put(`${this.Constants.org.baseURL}/configuration/custom-user-fields` + '/' + field.key, this.valuesAsList(field));
  }

  delete(field) {
    return this.$http.delete(`${this.Constants.org.baseURL}/configuration/custom-user-fields` + '/' + field.key);
  }
}

export default CustomUserFieldsService;
