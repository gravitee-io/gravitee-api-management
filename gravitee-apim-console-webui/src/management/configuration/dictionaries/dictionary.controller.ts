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

import { StateService } from '@uirouter/core';
import angular from 'angular';
import * as _ from 'lodash';

import DictionaryService from '../../../services/dictionary.service';
import NotificationService from '../../../services/notification.service';

interface IDictionaryScope extends ng.IScope {
  formDictionary: any;
}

class DictionaryController {
  private dictionary: any;
  private initialDictionary: any;
  private dictProperties: any;

  private joltSpecificationOptions: any;
  private requestBodyOptions: any;
  private providers: { id: string; name: string }[];
  private types: { id: string; name: string }[];
  private timeUnits: { id: string; name: string }[];

  private updateMode: boolean;

  private selectedProperties: any = {};

  private query: any;
  private selectAll: boolean;
  private formDictionary: any;

  constructor(
    private $scope: IDictionaryScope,
    private $state: StateService,
    private $mdEditDialog,
    private $mdDialog: angular.material.IDialogService,
    private NotificationService: NotificationService,
    private DictionaryService: DictionaryService,
  ) {
    'ngInject';

    this.types = [
      {
        id: 'MANUAL',
        name: 'Manual',
      },
      {
        id: 'DYNAMIC',
        name: 'Dynamic',
      },
    ];

    this.providers = [
      {
        id: 'HTTP',
        name: 'Custom (HTTP)',
      },
    ];

    this.timeUnits = [
      {
        id: 'seconds',
        name: 'Seconds',
      },
      {
        id: 'minutes',
        name: 'Minutes',
      },
      {
        id: 'hours',
        name: 'Hours',
      },
    ];

    this.joltSpecificationOptions = {
      placeholder: 'Edit your JOLT specification here.',
      lineWrapping: true,
      lineNumbers: true,
      allowDropFileTypes: true,
      autoCloseTags: true,
      mode: 'javascript',
      controller: this,
    };

    this.requestBodyOptions = {
      lineWrapping: true,
      lineNumbers: true,
      allowDropFileTypes: true,
      autoCloseTags: true,
      controller: this,
    };
  }

  $onInit() {
    this.updateMode = this.dictionary && this.dictionary.id;
    this.initialDictionary = _.cloneDeep(this.dictionary);
    this.dictProperties = this.computeProperties();

    this.query = {
      limit: 10,
      page: 1,
      total: (this.initialDictionary && this.initialDictionary.properties && Object.keys(this.initialDictionary.properties).length) || 0,
    };
  }

  getPropertiesPage = (reverse: boolean) => {
    const properties = this.dictProperties
      .sort((entry, entry2) => {
        if (reverse) {
          return entry2.key.localeCompare(entry.key);
        } else {
          return entry.key.localeCompare(entry2.key);
        }
      })
      .slice((this.query.page - 1) * this.query.limit, this.query.page * this.query.limit);

    return properties;
  };

  reset() {
    this.dictionary = _.cloneDeep(this.initialDictionary);
    this.dictProperties = this.computeProperties();
    this.formDictionary.$setPristine();
  }

  update() {
    if (!this.updateMode) {
      this.DictionaryService.create(this.dictionary).then((response: any) => {
        this.NotificationService.show('Dictionary ' + this.dictionary.name + ' has been created');
        this.$state.go('management.settings.dictionaries.dictionary', { dictionaryId: response.data.id }, { reload: true });
      });
    } else {
      this.DictionaryService.update(this.dictionary).then((response) => {
        this.NotificationService.show('Dictionary ' + this.dictionary.name + ' has been updated');
        this.dictionary = response.data;
        this.dictProperties = this.computeProperties();
      });
    }
  }

  delete() {
    this.$mdDialog
      .show({
        controller: 'DialogConfirmController',
        controllerAs: 'ctrl',
        template: require('../../../components/dialog/confirmWarning.dialog.html'),
        clickOutsideToClose: true,
        locals: {
          title: 'Are you sure you want to delete this dictionary?',
          confirmButton: 'Yes, delete it',
        },
      })
      .then((response) => {
        if (response) {
          this.DictionaryService.delete(this.dictionary).then(() => {
            this.NotificationService.show('Dictionary ' + this.dictionary.name + ' has been deleted');
            this.$state.go('management.settings.dictionaries.list', {}, { reload: true });
          });
        }
      });
  }

  deploy() {
    this.DictionaryService.deploy(this.dictionary).then((response) => {
      this.NotificationService.show('Dictionary ' + this.dictionary.name + ' has been deployed');
      this.dictionary = response.data;
      this.dictProperties = this.computeProperties();
    });
  }

  start() {
    this.DictionaryService.start(this.dictionary).then((response) => {
      this.NotificationService.show('Dictionary ' + this.dictionary.name + ' has been started');
      this.dictionary = response.data;
      this.dictProperties = this.computeProperties();
    });
  }

  stop() {
    this.DictionaryService.stop(this.dictionary).then((response) => {
      this.NotificationService.show('Dictionary ' + this.dictionary.name + ' has been stopped');
      this.dictionary = response.data;
      this.dictProperties = this.computeProperties();
    });
  }

  hasProperties() {
    return this.dictionary.properties && Object.keys(this.dictionary.properties).length > 0;
  }

  addProperty() {
    this.$mdDialog
      .show({
        controller: 'DialogAddPropertyController',
        controllerAs: 'dialogDictionaryAddPropertyCtrl',
        template: require('./add-property.dialog.html'),
        clickOutsideToClose: true,
      })
      .then((property) => {
        if (this.dictionary.properties === undefined) {
          this.dictionary.properties = {};
        }

        if (property) {
          this.dictionary.properties[property.key] = property.value;
          ++this.query.total;
        }

        this.dictProperties = this.computeProperties();
      });
  }

  editProperty(event, key, value) {
    event.stopPropagation();

    this.$mdEditDialog.small({
      modelValue: value,
      placeholder: 'Set property value',
      save: (input) => {
        this.dictionary.properties[key] = input.$modelValue;
      },
      targetEvent: event,
      validators: {
        'md-maxlength': 160,
      },
    });
  }

  deleteProperty(key) {
    delete this.dictionary.properties[key];
    --this.query.total;
    this.dictProperties = this.computeProperties();
  }

  deleteSelectedProperties() {
    _.forEach(this.selectedProperties, (v, k) => {
      if (v) {
        this.deleteProperty(k);
        delete this.selectedProperties[k];
      }
    });
  }

  saveProperties() {
    this.DictionaryService.update(this.dictionary).then((response) => {
      this.NotificationService.show('Properties has been updated');
      this.dictionary = response.data;
      this.dictProperties = this.computeProperties();
    });
  }

  toggleSelectAll(selectAll) {
    if (selectAll) {
      _.forEach(this.dictionary.properties, (v, k) => (this.selectedProperties[k] = true));
    } else {
      this.selectedProperties = {};
    }
  }

  checkSelectAll() {
    this.selectAll = _.filter(this.selectedProperties, (p) => p).length === Object.keys(this.dictionary.properties).length;
  }

  hasSelectedProperties() {
    return _.filter(this.selectedProperties, (p) => p).length > 0;
  }

  addHTTPHeader() {
    if (this.dictionary.provider.configuration.headers === undefined) {
      this.dictionary.provider.configuration.headers = [];
    }

    this.dictionary.provider.configuration.headers.push({ name: '', value: '' });
  }

  removeHTTPHeader(idx) {
    if (this.dictionary.provider.configuration.headers !== undefined) {
      this.dictionary.provider.configuration.headers.splice(idx, 1);
      this.formDictionary.$setDirty();
    }
  }

  computeProperties = () => {
    return Object.entries((this.dictionary && this.dictionary.properties) || {}).map((entry) => {
      const result: any = {};
      result.key = entry[0];
      result.value = entry[1];
      return result;
    });
  };

  getHttpMethods() {
    return ['GET', 'DELETE', 'PATCH', 'POST', 'PUT', 'OPTIONS', 'TRACE', 'HEAD'];
  }
}

export default DictionaryController;
