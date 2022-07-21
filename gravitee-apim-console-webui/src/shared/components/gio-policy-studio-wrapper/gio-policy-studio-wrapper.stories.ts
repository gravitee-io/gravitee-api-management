/* eslint-disable @typescript-eslint/no-var-requires */
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
import { CUSTOM_ELEMENTS_SCHEMA } from '@angular/core';
import { moduleMetadata, StoryObj } from '@storybook/angular';
import { of } from 'rxjs';

import { GioPolicyStudioWrapperComponent } from './gio-policy-studio-wrapper.component';
import { GioPolicyStudioWrapperModule } from './gio-policy-studio-wrapper.module';

import { FlowService } from '../../../services-ngx/flow.service';
import { PolicyService } from '../../../services-ngx/policy.service';
import { ResourceService } from '../../../services-ngx/resource.service';
import { SpelService } from '../../../services-ngx/spel.service';
import { fakeGrammar } from '../../../entities/spel/grammar.fixture';

const apimPolicies = require('./stories-resources/apim-policies.json');
const apimResourceTypes = require('./stories-resources/apim-resource-types.json');
const apimDefinition = require('./stories-resources/apim-definition.json');
const apimFlow = require('./stories-resources/apim-flow.json');
const apimConfiguration = require('./stories-resources/apim-configuration.json');
const apimPropertyProviders = require('./stories-resources/apim-property-providers.json');

export default {
  title: 'Shared / Policy Studio Wrapper',
  component: GioPolicyStudioWrapperComponent,
  decorators: [
    moduleMetadata({
      imports: [GioPolicyStudioWrapperModule],
      providers: [
        {
          provide: FlowService,
          useValue: {
            getConfigurationSchemaForm: () => of(apimConfiguration),
          },
        },
        {
          provide: PolicyService,
          useValue: {
            getDocumentation: (id: string) => of(buildDoc(`${id} documentation`)),
          },
        },
        {
          provide: ResourceService,
          useValue: {
            getDocumentation: (id: string) => of(buildDoc(`${id} documentation`)),
          },
        },
        {
          provide: SpelService,
          useValue: {
            getGrammar: () => of(fakeGrammar()),
          },
        },
      ],
      schemas: [CUSTOM_ELEMENTS_SCHEMA],
    }),
  ],
};

const buildDoc = (title) => {
  return `= ${title}

== Phase

|===
|onRequest|onResponse

|X
|

|===

== Description

Mock documentation in story...

[source, xml]
.Body content example (xml)
----
<user id="{#request.paths[3]}">
    <firstname>{#properties['firstname_' + #request.paths[3]]}</firstname>
\t<lastname>{#properties['lastname_' + #request.paths[3]]}</lastname>
\t<age>{(T(java.lang.Math).random() * 60).intValue()}</age>
\t<createdAt>{(new java.util.Date()).getTime()}</createdAt>
</user>
----
      `;
};

const save = (event) => {
  event.target.definition = event.detail.definition;
};

export const Default: StoryObj = {
  render: (props) => ({
    template: `<gio-policy-studio-wrapper
      [canAdd]='canAdd'
      [canDebug]='canDebug'
      [hasResources]='hasResources'
      [hasPolicyFilter]='hasPolicyFilter'
      [hasProperties]='hasProperties'
      [sortable]='sortable'
      [policies]='policies'
      [definition]='definition'
      [services]='services'
      [flowSchema]='flowSchema'
      [resourceTypes]='resourceTypes'
      [flowsTitle]='flowsTitle'
      [propertyProviders]='propertyProviders'
      [readonlyPlans]='readonlyPlans'
      [dynamicPropertySchema]='dynamicPropertySchema'
      [debugResponse]='debugResponse'
      [configurationInformation]='configurationInformation'
      >
      </gio-policy-studio-wrapper>`,
    props,
  }),
  args: {
    policies: apimPolicies.data,
    services: {},
    resourceTypes: apimResourceTypes.data,
    propertyProviders: apimPropertyProviders.data,
    definition: apimDefinition,
    flowSchema: apimFlow,
    configurationInformation:
      'By default, the selection of a flow is based on the operator defined in the flow itself.' +
      ' This operator allows either to select a flow when the path matches exactly, or when the start of the path matches.' +
      ' The "Best match" option allows you to select the flow from the path that is closest.',
    flowsTitle: 'API Flows',
    hasResources: true,
    hasProperties: true,
    hasPolicyFilter: true,
    readonlyPlans: false,
    canAdd: true,
    sortable: true,
    '@gv-policy-studio:save': save.bind(this),
    canDebug: true,
    dynamicPropertySchema: {},
    debugResponse: {
      isLoading: true,
      request: {
        path: '/',
        method: 'GET',
        body: '',
      },
      response: {
        body:
          '{"swagger":"2.0","info":{"title":"Solar System openData","description":"API to get all data about all solar system objects","version":"1.2.0"},"host":"api.le-systeme-solaire.net","basePath":"/rest.php","schemes":["https"],"consumes":["application/json"],"produces":["application/json"],"tags":[{"name":"bodies","description":"Object with all data about the concerned body : orbitals, physicals and atmosphere"},{"name":"knowncount","description":"Count of known objects"}],"paths":{"/bodies":{"get":{"tags":["bodies"],"summary":"List","parameters":[{"name":"data","in":"query","description":"The data you want to retrieve (comma separated). Example: id,semimajorAxis,isPlanet.","required":false,"type":"string"},{"name":"exclude","in":"query","description":"One or more data you want to exclude (comma separated). Example: id,isPlanet.","required":false,"type":"string"},{"name":"order","in":"query","description":"A data you want to sort on and the sort direction (comma separated). Example: id,desc. Only one data is authorized.","required":false,"type":"string"},{"name":"page","in":"query","description":"Page number (number>=1) and page size (size>=1 and 20 by default) (comma separated). NB: You cannot use \\"page\\" without \\"order\\"! Example: 1,10.","required":false,"type":"string"},{"name":"rowData","in":"query","description":"Transform the object in records. NB: This can also be done client-side in JavaScript!","required":false,"type":"boolean"},{"name":"filter[]","in":"query","description":"Filters to be applied. Each filter consists of a data, an operator and a value (comma separated). Example: id,eq,mars. Accepted operators are : cs (like) - sw (start with) - ew (end with) - eq (equal) - lt (less than) - le (less or equal than) - ge (greater or equal than) - gt (greater than) - bt (between). And all opposites operators : ncs - nsw - new - neq - nlt - nle - nge - ngt - nbt. Note : if anyone filter is invalid, all filters will be ignore.","required":false,"type":"array","collectionFormat":"multi","items":{"type":"string"}},{"name":"satisfy","in":"query","description":"Should all filters match (default)? Or any?","required":false,"type":"string","enum":["any"]}],"responses":{"200":{"description":"An array of bodies","schema":{"type": "object","properties": {"bodies": {"type":"array","items":{"type": "object","properties": {"id": {"type": "string"},"name": {"type": "string"},"englishName": {"type": "string"},"isPlanet": {"type": "boolean"},"moons":{"type":"array", "items":{"type":"object", "properties": {"moon" :{"type":"string"}, "rel" :{"type":"string"}}}},"semimajorAxis": {"type": "number"},"perihelion": {"type": "number"},"aphelion": {"type": "number"},"eccentricity": {"type": "number"},"inclination": {"type": "number"},"mass":{"type":"object", "properties":{ "massValue" :{"type":"number"}, "massExponent" :{"type":"integer"}}},"vol":{"type":"object", "properties":{ "volValue" :{"type":"number"}, "volExponent" :{"type":"integer"}}},"density": {"type": "number"},"gravity": {"type": "number"},"escape": {"type": "number"},"meanRadius": {"type": "number"},"equaRadius": {"type": "number"},"polarRadius": {"type": "number"},"flattening": {"type": "number"},"dimension": {"type": "string"},"sideralOrbit": {"type": "number"},"sideralRotation": {"type": "number"},"aroundPlanet":{"type":"object", "properties":{ "planet" :{"type":"string"}, "rel" :{"type":"string"}}},"discoveredBy": {"type": "string"},"discoveryDate": {"type": "string"},"alternativeName": {"type": "string"},"axialTilt": {"type": "number"},"avgTemp": {"type": "number"},"mainAnomaly": {"type": "number"},"argPeriapsis": {"type": "number"},"longAscNode": {"type": "number"},"rel":{"type":"string"}}}}}}}}}},"/bodies/{id}":{"get":{"tags":["bodies"],"summary":"read","parameters":[{"name":"id","in":"path","description":"Identifier for item.","required":true,"type":"string"}],"responses":{"200":{"description":"The requested item.","schema":{"type": "object","properties": {"id": {"type": "string"},"name": {"type": "string"},"englishName": {"type": "string"},"isPlanet": {"type": "boolean"},"moons":{"type":"array", "items":{"type":"object", "properties": {"moon" :{"type":"string"}, "rel" :{"type":"string"}}}},"semimajorAxis": {"type": "number"},"perihelion": {"type": "number"},"aphelion": {"type": "number"},"eccentricity": {"type": "number"},"inclination": {"type": "number"},"mass":{"type":"object", "properties":{ "massValue" :{"type":"number"}, "massExponent" :{"type":"integer"}}},"vol":{"type":"object", "properties":{ "volValue" :{"type":"number"}, "volExponent" :{"type":"integer"}}},"density": {"type": "number"},"gravity": {"type": "number"},"escape": {"type": "number"},"meanRadius": {"type": "number"},"equaRadius": {"type": "number"},"polarRadius": {"type": "number"},"flattening": {"type": "number"},"dimension": {"type": "string"},"sideralOrbit": {"type": "number"},"sideralRotation": {"type": "number"},"aroundPlanet":{"type":"object", "properties":{ "planet" :{"type":"string"}, "rel" :{"type":"string"}}},"discoveredBy": {"type": "string"},"discoveryDate": {"type": "string"},"alternativeName": {"type": "string"},"axialTilt": {"type": "number"},"avgTemp": {"type": "number"},"mainAnomaly": {"type": "number"},"argPeriapsis": {"type": "number"},"longAscNode": {"type": "number"}}}}}}},"/knowncount":{"get":{"tags":["knowncount"],"summary":"List","parameters":[{"name":"rowData","in":"query","description":"Transform the object in records. NB: This can also be done client-side in JavaScript!","required":false,"type":"boolean"}],"responses":{"200":{"description":"An array of knowncount","schema":{"type": "object","properties": {"knowncount": {"type":"array","items":{"type": "object","properties": {"id": {"type": "string"},"knownCount": {"type": "number"},"updateDate": {"type": "string"},"rel":{"type":"string"}}}}}}}}}},"/knowncount/{id}":{"get":{"tags":["knowncount"],"summary":"read","parameters":[{"name":"id","in":"path","description":"Identifier for item.","required":true,"type":"string"}],"responses":{"200":{"description":"The requested item.","schema":{"type": "object","properties": {"id": {"type": "string"},"knownCount": {"type": "number"},"updateDate": {"type": "string"}}}}}}}}}',
        headers: {
          'transfer-encoding': 'chunked',
          Server: 'Apache',
          'Access-Control-Allow-Origin': '*',
          'Access-Control-Allow-Credentials': 'true',
          Connection: 'keep-alive',
          'X-Gravitee-Transaction-Id': 'dc08a805-76c9-4f27-88a8-0576c9df2790',
          'X-Gravitee-Request-Id': 'dc08a805-76c9-4f27-88a8-0576c9df2790',
          Date: 'Wed, 11 Aug 2021 12:29:05 GMT',
          'Content-Type': 'application/json; charset=utf-8',
        },
        statusCode: 200,
      },
    },
  },
};
