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
// eslint-disable-next-line @typescript-eslint/no-var-requires
const copy = require('clipboard-copy');
// eslint-disable-next-line @typescript-eslint/no-var-requires
const JsDiff = require('diff/dist/diff.min.js');
import '@gravitee/ui-components/wc/gv-policy-studio';
import '@gravitee/ui-components/wc/gv-switch';
import '@gravitee/ui-components/wc/gv-popover';
import * as _ from 'lodash';
import * as angular from 'angular';
import { StateService } from '@uirouter/core';
import { propertyProviders } from '../../design/design/design.controller';

enum Modes {
  Diff = 'Diff',
  DiffWithMaster = 'DiffWithMaster',
  Payload = 'Payload',
  Design = 'Design',
}

class ApiHistoryController {
  public modes = Modes;
  public modeOptions: any;
  private studio: any;
  private mode: string;
  private api: any;
  private events: any;
  private eventsSelected: any;
  private eventsTimeline: any;
  private eventsToCompare: any;
  private eventSelected: any;
  private eventToCompareRequired: boolean;
  private eventTypes: string;
  private apisSelected: any;
  private eventSelectedPayloadDefinition: any;
  private eventSelectedPayload: any;
  private right: any;
  private left: any;
  private added: number;
  private removed: number;

  constructor(
    private $mdDialog: ng.material.IDialogService,
    private $scope: any,
    private $rootScope: ng.IRootScopeService,
    private $state: StateService,
    private ApiService,
    private NotificationService,
    private resolvedEvents,
    private PolicyService,
    private ResourceService,
    private FlowService,
  ) {
    'ngInject';
    this.api = JSON.parse(angular.toJson(_.cloneDeep(this.$scope.$parent.apiCtrl.api)));
    this.events = resolvedEvents.data;
    this.eventsSelected = [];
    this.eventsTimeline = [];
    this.eventsToCompare = [];
    this.eventSelected = {};
    this.mode = this.hasDesign() ? Modes.Design : Modes.Payload;
    this.eventToCompareRequired = false;
    this.eventTypes = 'PUBLISH_API';
    this.modeOptions = [
      { title: Modes.Design, id: Modes.Design },
      { title: Modes.Payload, id: Modes.Payload },
    ];
  }

  $onInit() {
    this.studio = document.querySelector('gv-policy-studio');
    if (this.hasDesign()) {
      Promise.all([
        this.PolicyService.list(true, true),
        this.ResourceService.list(true, true),
        this.ApiService.getFlowSchemaForm(),
        this.FlowService.getConfigurationSchema(),
      ]).then(([policies, resources, flowSchema, configurationSchema]) => {
        this.studio.policies = policies.data;
        this.studio.resourceTypes = resources.data;
        this.studio.flowSchema = flowSchema.data;
        this.studio.configurationSchema = configurationSchema.data;
        this.studio.propertyProviders = propertyProviders;
      });
    }
    this.init();
    this.initTimeline(this.events);
  }

  init() {
    this.$scope.$parent.apiCtrl.checkAPISynchronization(this.api);
    this.$scope.$on('apiChangeSuccess', (event, args) => {
      if (this.$state.current.name.endsWith('history')) {
        // reload API
        this.api = JSON.parse(angular.toJson(_.cloneDeep(args.api)));
        // reload API events
        this.ApiService.getApiEvents(this.api.id, this.eventTypes).then((response) => {
          this.events = response.data;
          this.reloadEventsTimeline(this.events);
        });
      }
    });
    this.$scope.$on('checkAPISynchronizationSucceed', () => {
      this.reloadEventsTimeline(this.events);
    });
  }

  setEventToStudio(eventTimeline, api) {
    this.studio.definition = {
      version: api.version,
      flows: api.flows != null ? api.flows : [],
      resources: api.resources,
      plans: api.plans != null ? api.plans : [],
      properties: api.properties,
      flow_mode: api.flow_mode,
    };
    this.studio.services = api.services || {};
  }

  initTimeline(events) {
    this.eventsTimeline = events.map((event) => ({
      event: event,
      badgeClass: 'info',
      badgeIconClass: 'glyphicon-check',
      title: event.type,
      when: event.created_at,
      user: event.user,
      deploymentLabel: event.properties.deployment_label,
      deploymentNumber: event.properties.deployment_number,
    }));
  }

  selectEvent(_eventTimeline) {
    if (this.eventToCompareRequired) {
      this.diff(_eventTimeline);
      this.selectEventToCompare(_eventTimeline);
    } else {
      this.mode = this.hasDesign() ? Modes.Design : Modes.Payload;
      this.apisSelected = [];
      this.eventsSelected = [];
      this.clearDataToCompare();

      const idx = this.eventsSelected.indexOf(_eventTimeline);
      if (idx > -1) {
        this.eventsSelected.splice(idx, 1);
      } else {
        this.eventsSelected.push(_eventTimeline);
      }
      if (this.eventsSelected.length > 0) {
        const eventSelected = this.eventsSelected[0];
        this.eventSelectedPayload = JSON.parse(eventSelected.event.payload);
        this.eventSelectedPayloadDefinition = this.reorganizeEvent(this.eventSelectedPayload);

        this.setEventToStudio(_eventTimeline, this.eventSelectedPayloadDefinition);
      }
    }
  }

  selectEventToCompare(_eventTimeline) {
    this.eventsToCompare.push(_eventTimeline);
  }

  clearDataToCompare() {
    this.eventsToCompare = [];
  }

  clearDataSelected() {
    this.eventsSelected = [];
  }

  isEventSelectedForComparaison(_event) {
    return this.eventsToCompare.indexOf(_event) > -1;
  }

  diffWithMaster() {
    if (this.mode === Modes.DiffWithMaster) {
      this.mode = null;
      this.mode = Modes.Payload;
      this.clearDataToCompare();
    } else {
      this.mode = null;
      this.mode = Modes.DiffWithMaster;
      this.eventToCompareRequired = false;
      this.clearDataToCompare();
      const latestEvent = this.events[0];
      if (this.eventsSelected.length > 0) {
        if (this.eventsSelected[0].isCurrentAPI) {
          this.right = this.reorganizeEvent(JSON.parse(this.eventsSelected[0].event.payload));
          this.left = this.reorganizeEvent(JSON.parse(latestEvent.payload));
        } else {
          this.left = this.reorganizeEvent(JSON.parse(this.eventsSelected[0].event.payload));
          this.right = this.reorganizeEvent(JSON.parse(latestEvent.payload));
        }
        this.updateDiffStats();
      }
    }
  }

  computeLines(part) {
    if (part && part.value) {
      return part.value.split('\n').length - 1;
    }
    return 0;
  }

  updateDiffStats() {
    this.added = 0;
    this.removed = 0;
    const diff = JsDiff.diffJson(this.left, this.right);
    diff.forEach((part) => {
      if (part.added) {
        this.added += this.computeLines(part);
      } else if (part.removed) {
        this.removed += this.computeLines(part);
      }
    });
  }

  hasDiff() {
    return this.mode === this.modes.Diff || this.mode === this.modes.DiffWithMaster;
  }

  enableDiff() {
    this.clearDataToCompare();
    this.eventToCompareRequired = true;
  }

  disableDiff() {
    this.eventToCompareRequired = false;
  }

  hasDesign() {
    return this.api != null && this.api.gravitee != null && this.api.gravitee === '2.0.0';
  }

  copyToClipboard(event) {
    copy(JSON.stringify(this.eventSelectedPayloadDefinition, null, 2));
    const clipboardIcon = event.target.icon;
    event.target.icon = 'communication:clipboard-check';
    setTimeout(() => {
      event.target.icon = clipboardIcon;
    }, 1000);
  }

  toggleMode({ detail }) {
    if (detail === false) {
      this.clearDataToCompare();
      this.eventToCompareRequired = false;
      this.mode = Modes.Design;
    } else {
      this.mode = Modes.Payload;
    }
  }

  diff(eventTimeline) {
    this.mode = Modes.Diff;
    if (this.eventsSelected.length > 0) {
      if (eventTimeline.isCurrentAPI) {
        this.left = this.reorganizeEvent(JSON.parse(this.eventsSelected[0].event.payload));
        this.right = this.reorganizeEvent(JSON.parse(eventTimeline.event.payload));
      } else {
        const event1UpdatedAt = eventTimeline.event.updated_at;
        const event2UpdatedAt = this.eventsSelected[0].event.updated_at;
        const eventSelected = this.reorganizeEvent(JSON.parse(this.eventsSelected[0].event.payload));

        if (event1UpdatedAt > event2UpdatedAt) {
          this.left = eventSelected;
          this.right = this.reorganizeEvent(JSON.parse(eventTimeline.event.payload));
        } else {
          this.left = this.reorganizeEvent(JSON.parse(eventTimeline.event.payload));
          this.right = eventSelected;
        }
      }
      this.updateDiffStats();
    }
    this.disableDiff();
  }

  isEventSelected(_eventTimeline) {
    return this.eventsSelected.indexOf(_eventTimeline) > -1;
  }

  rollback(_apiPayload) {
    const _apiDefinition = JSON.parse(_apiPayload.definition);
    _apiDefinition.id = this.api.id;
    delete _apiDefinition.deployed_at;
    _apiDefinition.description = _apiPayload.description;
    _apiDefinition.visibility = _apiPayload.visibility;

    this.ApiService.rollback(this.api.id, _apiDefinition).then(() => {
      this.NotificationService.show('Api rollback !');

      this.ApiService.get(this.api.id).then((response) => {
        this.$rootScope.$broadcast('apiChangeSuccess', { api: response.data });
      });
    });
  }

  showRollbackAPIConfirm(ev, api) {
    ev.stopPropagation();
    this.$mdDialog
      .show({
        controller: 'DialogConfirmController',
        controllerAs: 'ctrl',
        template: require('../../../../components/dialog/confirm.dialog.html'),
        clickOutsideToClose: true,
        locals: {
          title: 'Would you like to rollback your API?',
          confirmButton: 'Rollback',
        },
      })
      .then((response) => {
        if (response) {
          this.rollback(api);
        }
      });
  }

  stringifyCurrentApi() {
    const payload = _.cloneDeep(this.api);
    // Because server add "/" to virtual_hosts at deploy
    payload.proxy.virtual_hosts = payload.proxy.virtual_hosts.map((host) => {
      if (!host.path.endsWith('/')) {
        host.path = `${host.path}/`;
      }
      return host;
    });

    delete payload.deployed_at;
    delete payload.created_at;
    delete payload.updated_at;
    delete payload.visibility;
    delete payload.state;
    delete payload.permission;
    delete payload.owner;
    delete payload.picture_url;
    delete payload.background_url;
    delete payload.categories;
    delete payload.groups;
    delete payload.etag;
    delete payload.context_path;
    delete payload.disable_membership_notifications;
    delete payload.labels;
    delete payload.entrypoints;
    delete payload.lifecycle_state;
    delete payload.path_mappings;
    delete payload.tags;
    delete payload.workflow_state;
    delete payload.response_templates;

    return JSON.stringify({ definition: JSON.stringify(payload) });
  }

  reloadEventsTimeline(events) {
    this.clearDataSelected();
    this.initTimeline(events);
    if (!this.$scope.$parent.apiCtrl.apiIsSynchronized && !this.$scope.$parent.apiCtrl.apiJustDeployed) {
      this.eventsTimeline.unshift({
        event: {
          payload: this.stringifyCurrentApi(),
        },
        badgeClass: 'warning',
        badgeIconClass: 'glyphicon-refresh',
        title: 'TO_DEPLOY',
        isCurrentAPI: true,
      });
    }
    this.selectEvent(this.eventsTimeline[0]);
  }

  reorganizeEvent(_event) {
    const eventPayloadDefinition = JSON.parse(_event.definition);
    const reorganizedEvent = {
      ...eventPayloadDefinition,
      name: eventPayloadDefinition.name,
      version: eventPayloadDefinition.version,
      description: _event.description != null ? _event.description : eventPayloadDefinition.description,
      tags: eventPayloadDefinition.tags,
      proxy: eventPayloadDefinition.proxy,
      paths: eventPayloadDefinition.paths,
      plans: eventPayloadDefinition.plans,
      flows: eventPayloadDefinition.flows,
      properties: eventPayloadDefinition.properties,
      services: eventPayloadDefinition.services,
      resources: eventPayloadDefinition.resources,
      path_mappings: eventPayloadDefinition.path_mappings,
      response_templates: eventPayloadDefinition.response_templates,
    };
    if (reorganizedEvent.flow_mode != null) {
      reorganizedEvent.flow_mode = reorganizedEvent.flow_mode.toLowerCase();
    }
    return reorganizedEvent;
  }

  fetchPolicyDocumentation({ detail }) {
    const policy = detail.policy;
    this.PolicyService.getDocumentation(policy.id)
      .then((response) => {
        this.studio.documentation = { content: response.data, image: policy.icon, id: policy.id };
      })
      .catch(() => (this.studio.documentation = null));
  }

  fetchResourceDocumentation(event) {
    const {
      detail: { resourceType, target },
    } = event;
    this.ResourceService.getDocumentation(resourceType.id)
      .then((response) => {
        target.documentation = { content: response.data, image: resourceType.icon };
      })
      .catch(() => (target.documentation = null));
  }
}

export default ApiHistoryController;
