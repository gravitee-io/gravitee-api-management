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
import '@gravitee/ui-components/wc/gv-policy-studio';
import '@gravitee/ui-components/wc/gv-switch';
import '@gravitee/ui-components/wc/gv-popover';
import * as angular from 'angular';

import { ActivatedRoute } from '@angular/router';
import { cloneDeep, isEmpty } from 'lodash';

import { ApiService } from '../../../../services/api.service';
import { ApiV2Service } from '../../../../services-ngx/api-v2.service';
import { GroupV2Service } from '../../../../services-ngx/group-v2.service';

// eslint-disable-next-line @typescript-eslint/no-var-requires
const copy = require('clipboard-copy');
// eslint-disable-next-line @typescript-eslint/no-var-requires
const JsDiff = require('diff/dist/diff.min.js');

const propertyProviders = [
  {
    id: 'HTTP',
    name: 'Custom (HTTP)',
    schema: {
      type: 'object',
      properties: {
        method: {
          title: 'HTTP Method',
          description: 'HTTP method to invoke the endpoint.',
          type: 'string',
          default: 'GET',
          enum: ['GET', 'POST', 'PUT', 'DELETE', 'PATCH', 'HEAD', 'CONNECT', 'OPTIONS', 'TRACE'],
        },
        url: {
          title: 'Http service URL',
          description: 'http://localhost',
          type: 'string',
          pattern: '^(http://|https://)',
        },
        useSystemProxy: {
          title: 'Use system proxy',
          description: 'Use the system proxy configured by your administrator.',
          type: 'boolean',
        },
        headers: {
          type: 'array',
          title: 'Request Headers',
          items: {
            type: 'object',
            title: 'Header',
            properties: {
              name: {
                title: 'Name',
                type: 'string',
              },
              value: {
                title: 'Value',
                type: 'string',
              },
            },
          },
        },
        body: {
          title: 'Request body',
          type: 'string',
          'x-schema-form': {
            type: 'codemirror',
            codemirrorOptions: {
              lineWrapping: true,
              lineNumbers: true,
              allowDropFileTypes: true,
              autoCloseTags: true,
            },
          },
        },
        specification: {
          title: 'Transformation (JOLT Specification)',
          type: 'string',
          'x-schema-form': {
            type: 'codemirror',
            codemirrorOptions: {
              lineWrapping: true,
              lineNumbers: true,
              allowDropFileTypes: true,
              autoCloseTags: true,
              mode: 'javascript',
            },
          },
        },
      },
      required: ['url', 'specification'],
    },
    documentation:
      '= Custom (HTTP)\n\n=== How to ?\n\n 1. Set `Polling frequency interval` and `Time unit`\n2. Set the `HTTP service URL`\n 3. If the HTTP service doesn\'t return the expected output, add a JOLT `transformation` \n\n[source, json]\n----\n[\n  {\n    "key": 1,\n    "value": "https://north-europe.company.com/"\n  },\n  {\n    "key": 2,\n    "value": "https://north-europe.company.com/"\n  },\n  {\n    "key": 3,\n    "value": "https://south-asia.company.com/"\n  }\n]\n----\n',
  },
];

enum Modes {
  Diff = 'Diff',
  DiffWithMaster = 'DiffWithMaster',
  Payload = 'Payload',
  Design = 'Design',
}

class ApiHistoryControllerAjs {
  public activatedRoute: ActivatedRoute;
  public modes = Modes;
  public modeOptions: any;
  private studio: any;
  private mode: string;
  private api: any;
  private groups: any;
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
  private eventPage: number;
  private eventPageSize = 100;
  public hasNextEventPageToLoad = false;

  constructor(
    private $mdDialog: ng.material.IDialogService,
    private ApiService: ApiService,
    private NotificationService,
    private PolicyService,
    private ResourceService,
    private FlowService,
    private ngApiV2Service: ApiV2Service,
    private ngGroupV2Service: GroupV2Service,
  ) {
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
    Promise.all([
      this.ngGroupV2Service.list(1, 99999).toPromise(),
      this.ApiService.get(this.activatedRoute.snapshot.params.apiId),
      this.ApiService.isAPISynchronized(this.activatedRoute.snapshot.params.apiId),
    ]).then(([groups, api, apiIsSynchronizedResult]) => {
      this.api = api.data;
      this.groups = groups.data;

      this.eventPage = -1;
      this.events = [];
      const toDeployEventTimeline = !apiIsSynchronizedResult.data.is_synchronized
        ? {
            event: {
              payload: this.stringifyCurrentApi(),
            },
            badgeClass: 'warning',
            badgeIconClass: 'notification:sync',
            title: 'TO_DEPLOY',
            isCurrentAPI: true,
          }
        : undefined;
      this.appendNextPage(toDeployEventTimeline);
    });
  }

  async appendNextPage(toDeployEventTimeline?: any) {
    this.eventPage++;
    await this.ApiService.searchApiEvents(
      this.eventTypes,
      this.api.id,
      undefined,
      undefined,
      this.eventPage,
      this.eventPageSize,
      true,
    ).then((response) => {
      this.events = [...(this.events ?? []), ...response.data.content];
      this.hasNextEventPageToLoad =
        response.data.totalElements > response.data.pageNumber * this.eventPageSize + response.data.pageElements;
      this.eventsTimeline = this.events.map((event) => ({
        event: event,
        badgeClass: 'info',
        badgeIconClass: 'action:check_circle',
        title: event.type,
        when: event.created_at,
        user: event.user,
        deploymentLabel: event.properties.deployment_label,
        deploymentNumber: event.properties.deployment_number,
      }));
      if (toDeployEventTimeline) {
        this.eventsTimeline.unshift(toDeployEventTimeline);
      }
    });
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

    Promise.allSettled([
      this.ApiService.picture(this.api.id, this.api.updated_at),
      this.ApiService.background(this.api.id, this.api.updated_at),
    ])
      .then(([pictureResponse, backgroundResponse]) => {
        _apiDefinition.picture = pictureResponse.status === 'fulfilled' ? pictureResponse?.value : null;
        _apiDefinition.background = backgroundResponse.status === 'fulfilled' ? backgroundResponse?.value : null;

        return this.ApiService.rollback(this.api.id, { ..._apiDefinition, groups: this.listGroups(_apiPayload.groups) });
      })
      .then(() => {
        this.NotificationService.show('API successfully rollbacked!');

        return this.ApiService.get(this.api.id);
      })
      .then((response) => {
        this.api = JSON.parse(angular.toJson(cloneDeep(response.data)));
        // reload API events
        return this.ApiService.getApiEvents(this.api.id, this.eventTypes);
      })
      .then((response) => {
        this.events = response.data;
      })
      .then(() => this.ngApiV2Service.get(this.api.id).toPromise()); // To update the deploy banner
  }

  showRollbackAPIConfirm(ev, api) {
    ev.stopPropagation();
    this.$mdDialog
      .show({
        controller: 'DialogConfirmController',
        controllerAs: 'ctrl',
        template: require('html-loader!../../../../components/dialog/confirm.dialog.html').default, // eslint-disable-line @typescript-eslint/no-var-requires
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
    const payload = cloneDeep(this.api);

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
    delete payload.context_path;
    delete payload.disable_membership_notifications;
    delete payload.labels;
    delete payload.entrypoints;
    delete payload.lifecycle_state;
    delete payload.path_mappings;
    delete payload.tags;
    delete payload.workflow_state;
    delete payload.crossId;
    delete payload.definition_context;

    if (payload.response_templates && isEmpty(payload.response_templates)) {
      delete payload.response_templates;
    }

    if (payload.flows && isEmpty(payload.flows)) {
      delete payload.flows;
    }
    if (payload.resources && isEmpty(payload.resources)) {
      delete payload.resources;
    }
    if (payload.services && isEmpty(payload.services)) {
      delete payload.services;
    }

    payload.plans = (payload.plans ?? [])
      .filter((plan) => plan.status !== 'CLOSED')
      .map((plan) => {
        delete plan.characteristics;
        delete plan.comment_message;
        delete plan.comment_required;
        delete plan.created_at;
        delete plan.cross_id;
        delete plan.description;
        delete plan.excluded_groups;
        delete plan.need_redeploy_at;
        delete plan.general_conditions;
        delete plan.order;
        delete plan.published_at;
        delete plan.type;
        delete plan.updated_at;
        delete plan.closed_at;
        delete plan.validation;
        return plan;
      })
      .sort((plan) => plan.id);

    return JSON.stringify({ definition: JSON.stringify(payload) });
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
      plans: (eventPayloadDefinition.plans ?? []).sort((plan) => plan.id),
      flows: eventPayloadDefinition.flows,
      properties: eventPayloadDefinition.properties,
      services: eventPayloadDefinition.services,
      resources: eventPayloadDefinition.resources,
      path_mappings: eventPayloadDefinition.path_mappings,
      response_templates: eventPayloadDefinition.response_templates,
      groups: this.listGroups(_event.groups),
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

  private listGroups(groupIds: string[]): string[] {
    if (!groupIds) {
      return [];
    }
    return groupIds.map((groupId) => this.groups.find((group) => group.id === groupId)?.name).filter((groupName) => groupName != null);
  }
}
ApiHistoryControllerAjs.$inject = [
  '$mdDialog',
  'ApiService',
  'NotificationService',
  'PolicyService',
  'ResourceService',
  'FlowService',
  'ngApiV2Service',
  'ngGroupV2Service',
];

export default ApiHistoryControllerAjs;
