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

import { StateService } from "@uirouter/core";
import angular = require("angular");

import DictionaryService from "../../../services/dictionary.service";
import NotificationService from "../../../services/notification.service";
import _ = require('lodash');

interface IDictionaryScope extends ng.IScope {
  formDictionary: any;
}

class DictionaryController {

  private dictionary: any;
  private initialDictionary: any;

  private joltSpecificationOptions: any;
  private providers: {id: string; name: string}[];
  private types: {id: string; name: string}[];
  private timeUnits: {id: string; name: string}[];

  private updateMode: boolean;

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
        id: 'manual',
        name: 'Manual'
      },
      {
        id: 'dynamic',
        name: 'Dynamic'
      }
    ];

    this.providers = [
      {
        id: 'HTTP',
        name: 'Custom (HTTP)'
      }
    ];

    this.timeUnits = [
      {
        id: 'seconds',
        name: 'Seconds'
      },
      {
        id: 'minutes',
        name: 'Minutes'
      },
      {
        id: 'hours',
        name: 'Hours'
      }
    ];

    this.joltSpecificationOptions = {
      placeholder: "Edit your JOLT specification here.",
      lineWrapping: true,
      lineNumbers: true,
      allowDropFileTypes: true,
      autoCloseTags: true,
      mode: "javascript",
      controller: this
    };
  }

  $onInit() {
    this.updateMode = this.dictionary && this.dictionary.id;
    this.initialDictionary = _.cloneDeep(this.dictionary);
  }

  reset() {
    this.dictionary = _.cloneDeep(this.initialDictionary);
    this.$scope.formDictionary.$setPristine();
  }

  update() {
    if (!this.updateMode) {
      this.DictionaryService.create(this.dictionary).then((response) => {
        this.NotificationService.show('Dictionary ' + this.dictionary.name + ' has been created');
        this.$state.go('management.settings.dictionaries.dictionary', {dictionaryId: response.data.id}, {reload: true});
      });
    } else {
      this.DictionaryService.update(this.dictionary).then((response) => {
        this.NotificationService.show('Dictionary ' + this.dictionary.name + ' has been updated');
        this.dictionary = response.data;
      });
    }
  }

  delete() {
    this.DictionaryService.delete(this.dictionary).then((response) => {
      this.NotificationService.show('Dictionary ' + this.dictionary.name + ' has been deleted');
      this.$state.go('management.settings.dictionaries.list', {}, {reload: true});
    });
  }

  deploy() {
    this.DictionaryService.deploy(this.dictionary).then((response) => {
      this.NotificationService.show('Dictionary ' + this.dictionary.name + ' has been deployed');
      this.dictionary = response.data;
    });
  }

  start() {
    this.DictionaryService.start(this.dictionary).then((response) => {
      this.NotificationService.show('Dictionary ' + this.dictionary.name + ' has been started');
      this.dictionary = response.data;
    });
  }

  stop() {
    this.DictionaryService.stop(this.dictionary).then((response) => {
      this.NotificationService.show('Dictionary ' + this.dictionary.name + ' has been stopped');
      this.dictionary = response.data;
    });
  }

  hasProperties() {
    return this.dictionary.properties && Object.keys(this.dictionary.properties).length > 0;
  }

  addProperty() {
    this.$mdDialog.show({
      controller: 'DialogAddPropertyController',
      controllerAs: 'dialogDictionaryAddPropertyCtrl',
      template: require('./add-property.dialog.html'),
      clickOutsideToClose: true
    }).then( (property) => {
      if (this.dictionary.properties === undefined) {
        this.dictionary.properties = {};
      }

      if (property) {
        this.dictionary.properties[property.key] = property.value;
      }
    });
  }

  editProperty(event, key, value) {
    event.stopPropagation();

    this.$mdEditDialog.small({
      modelValue: value,
      placeholder: 'Set property value',
      save: (input) => { this.dictionary.properties[key] = input.$modelValue; },
      targetEvent: event,
      validators: {
        'md-maxlength': 160
      }
    });
  }

  deleteProperty(key) {
    delete this.dictionary.properties[key];
  }

  saveProperties() {
    this.DictionaryService.update(this.dictionary).then((response) => {
      this.NotificationService.show('Properties has been updated');
      this.dictionary = response.data;
    });
  }
}

export default DictionaryController;
