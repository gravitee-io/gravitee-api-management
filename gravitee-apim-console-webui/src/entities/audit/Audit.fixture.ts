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
import { Audit } from './Audit';

import { MetadataPage } from '../MetadataPage';

export function fakeMetadataPageAudit(attributes?: Partial<MetadataPage<Audit>>): MetadataPage<Audit> {
  const base: MetadataPage<Audit> = {
    content: [
      {
        id: '5efbd613-d4de-4f50-bbd6-13d4debf501f',
        referenceId: 'DEFAULT',
        referenceType: 'ENVIRONMENT',
        user: 'system',
        createdAt: 1650382350999,
        event: 'THEME_UPDATED',
        properties: {
          THEME: 'default',
        },
        patch: '[]',
      },
      {
        id: 'bb594c57-ef76-467d-994c-57ef76367d4e',
        referenceId: 'DEFAULT',
        referenceType: 'ENVIRONMENT',
        user: 'system',
        createdAt: 1650378937944,
        event: 'THEME_UPDATED',
        properties: {
          THEME: 'default',
        },
        patch: '[]',
      },
      {
        id: '5b3a86d6-2119-415f-ba86-d62119515f56',
        referenceId: 'DEFAULT',
        referenceType: 'ORGANIZATION',
        user: 'system',
        createdAt: 1650378937642,
        event: 'ROLE_UPDATED',
        properties: {
          ROLE: 'ORGANIZATION:ADMIN',
        },
        patch:
          '[{"op":"move","path":"/system","from":"/present"},{"op":"move","path":"/defaultRole","from":"/empty"},{"op":"add","path":"/permissions","value":[2315,1315,1115,1715,1015,2115,2015,2215,1215,1615,1815,1915,1415,1515]},{"op":"add","path":"/scope","value":"ORGANIZATION"},{"op":"add","path":"/name","value":"ADMIN"},{"op":"add","path":"/description","value":"System Role. Created by Gravitee.io"},{"op":"add","path":"/referenceType","value":"ORGANIZATION"},{"op":"add","path":"/id","value":"6b241a8d-d8cc-4f4d-a41a-8dd8cc0f4db3"},{"op":"add","path":"/referenceId","value":"DEFAULT"}]',
      },
      {
        id: 'de27272c-ba3e-45f9-a727-2cba3e05f92d',
        referenceId: '9c899adb-2af2-4f87-899a-db2af29f87da',
        referenceType: 'ENVIRONMENT',
        user: 'system',
        createdAt: 1650364860467,
        event: 'MEMBERSHIP_CREATED',
        properties: {
          USER: 'fb8b0c31-5428-4a4c-8b0c-315428da4c77',
        },
        patch:
          '[{"op":"add","path":"/roleId","value":"e4f62aca-f41d-40f6-b62a-caf41dc0f6ce"},{"op":"add","path":"/referenceType","value":"ENVIRONMENT"},{"op":"add","path":"/id","value":"c5601ce0-20fd-47d9-a01c-e020fd17d975"},{"op":"add","path":"/memberType","value":"USER"},{"op":"add","path":"/source","value":"cockpit"},{"op":"add","path":"/referenceId","value":"9c899adb-2af2-4f87-899a-db2af29f87da"},{"op":"add","path":"/memberId","value":"fb8b0c31-5428-4a4c-8b0c-315428da4c77"}]',
      },
      {
        id: 'e2bdb43a-94ec-4348-bdb4-3a94ecc34822',
        referenceId: '9c899adb-2af2-4f87-899a-db2af29f87da',
        referenceType: 'ENVIRONMENT',
        user: 'system',
        createdAt: 1650364860461,
        event: 'MEMBERSHIP_DELETED',
        properties: {
          USER: 'fb8b0c31-5428-4a4c-8b0c-315428da4c77',
        },
        patch:
          '[{"op":"remove","path":"/roleId"},{"op":"remove","path":"/referenceType"},{"op":"remove","path":"/id"},{"op":"remove","path":"/memberType"},{"op":"remove","path":"/source"},{"op":"remove","path":"/referenceId"},{"op":"remove","path":"/memberId"}]',
      },
      {
        id: 'f616b156-eb04-4c5a-96b1-56eb044c5a98',
        referenceId: 'DEFAULT',
        referenceType: 'ORGANIZATION',
        user: 'system',
        createdAt: 1650364860338,
        event: 'METADATA_UPDATED',
        properties: {
          METADATA: 'email',
          USER: 'fb8b0c31-5428-4a4c-8b0c-315428da4c77',
        },
        patch: '[]',
      },
      {
        id: 'fd3f47c9-0bd6-4e85-bf47-c90bd69e853e',
        referenceId: 'DEFAULT',
        referenceType: 'ORGANIZATION',
        user: 'system',
        createdAt: 1650364860331,
        event: 'METADATA_UPDATED',
        properties: {
          METADATA: 'picture',
          USER: 'fb8b0c31-5428-4a4c-8b0c-315428da4c77',
        },
        patch: '[]',
      },
      {
        id: 'b2d31d0b-f863-4ed2-931d-0bf8639ed22a',
        referenceId: 'DEFAULT',
        referenceType: 'ORGANIZATION',
        user: 'system',
        createdAt: 1650364860325,
        event: 'METADATA_UPDATED',
        properties: {
          METADATA: 'family_name',
          USER: 'fb8b0c31-5428-4a4c-8b0c-315428da4c77',
        },
        patch: '[]',
      },
      {
        id: '39668739-b56a-4fe9-a687-39b56adfe912',
        referenceId: 'DEFAULT',
        referenceType: 'ORGANIZATION',
        user: 'system',
        createdAt: 1650364860318,
        event: 'METADATA_UPDATED',
        properties: {
          METADATA: 'given_name',
          USER: 'fb8b0c31-5428-4a4c-8b0c-315428da4c77',
        },
        patch: '[]',
      },
      {
        id: '74df81f2-89e1-41f7-9f81-f289e1e1f77a',
        referenceId: 'DEFAULT',
        referenceType: 'ORGANIZATION',
        user: 'system',
        createdAt: 1650364860312,
        event: 'METADATA_UPDATED',
        properties: {
          METADATA: 'preferred_username',
          USER: 'fb8b0c31-5428-4a4c-8b0c-315428da4c77',
        },
        patch: '[]',
      },
      {
        id: '953d070d-148f-413c-bd07-0d148fd13cc5',
        referenceId: 'DEFAULT',
        referenceType: 'ORGANIZATION',
        user: 'system',
        createdAt: 1650364860306,
        event: 'METADATA_UPDATED',
        properties: {
          METADATA: 'sub',
          USER: 'fb8b0c31-5428-4a4c-8b0c-315428da4c77',
        },
        patch: '[]',
      },
      {
        id: 'e6efe570-5bf1-493d-afe5-705bf1293dd0',
        referenceId: 'DEFAULT',
        referenceType: 'ORGANIZATION',
        user: 'system',
        createdAt: 1650364860300,
        event: 'METADATA_UPDATED',
        properties: {
          METADATA: 'name',
          USER: 'fb8b0c31-5428-4a4c-8b0c-315428da4c77',
        },
        patch: '[]',
      },
      {
        id: '25fdae44-93f0-4adc-bdae-4493f07adc88',
        referenceId: 'DEFAULT',
        referenceType: 'ORGANIZATION',
        user: 'system',
        createdAt: 1650364860292,
        event: 'USER_UPDATED',
        properties: {
          USER: 'fb8b0c31-5428-4a4c-8b0c-315428da4c77',
        },
        patch: '[]',
      },
      {
        id: '605656f6-8c56-4103-9656-f68c56610375',
        referenceId: '9c899adb-2af2-4f87-899a-db2af29f87da',
        referenceType: 'ENVIRONMENT',
        user: 'system',
        createdAt: 1650364807563,
        event: 'MEMBERSHIP_CREATED',
        properties: {
          USER: 'f8f23327-30a2-4b51-b233-2730a27b5195',
        },
        patch:
          '[{"op":"add","path":"/roleId","value":"4c40b33d-6778-40bc-80b3-3d677860bc2a"},{"op":"add","path":"/referenceType","value":"ENVIRONMENT"},{"op":"add","path":"/id","value":"d4f69cfa-3497-42a7-b69c-fa349772a778"},{"op":"add","path":"/memberType","value":"USER"},{"op":"add","path":"/source","value":"cockpit"},{"op":"add","path":"/referenceId","value":"9c899adb-2af2-4f87-899a-db2af29f87da"},{"op":"add","path":"/memberId","value":"f8f23327-30a2-4b51-b233-2730a27b5195"}]',
      },
      {
        id: 'ee0b815d-1650-472f-8b81-5d1650672ff4',
        referenceId: '9c899adb-2af2-4f87-899a-db2af29f87da',
        referenceType: 'ENVIRONMENT',
        user: 'system',
        createdAt: 1650364807558,
        event: 'MEMBERSHIP_DELETED',
        properties: {
          USER: 'f8f23327-30a2-4b51-b233-2730a27b5195',
        },
        patch:
          '[{"op":"remove","path":"/roleId"},{"op":"remove","path":"/referenceType"},{"op":"remove","path":"/id"},{"op":"remove","path":"/memberType"},{"op":"remove","path":"/source"},{"op":"remove","path":"/referenceId"},{"op":"remove","path":"/memberId"}]',
      },
      {
        id: '4a22415b-1257-48f4-a241-5b1257a8f4dc',
        referenceId: '9c899adb-2af2-4f87-899a-db2af29f87da',
        referenceType: 'ENVIRONMENT',
        user: 'system',
        createdAt: 1650364807540,
        event: 'MEMBERSHIP_CREATED',
        properties: {
          USER: '16a7c47a-11df-46d1-a7c4-7a11dfe6d135',
        },
        patch:
          '[{"op":"add","path":"/roleId","value":"4c40b33d-6778-40bc-80b3-3d677860bc2a"},{"op":"add","path":"/referenceType","value":"ENVIRONMENT"},{"op":"add","path":"/id","value":"d934206d-806a-4151-b420-6d806ab151f6"},{"op":"add","path":"/memberType","value":"USER"},{"op":"add","path":"/source","value":"cockpit"},{"op":"add","path":"/referenceId","value":"9c899adb-2af2-4f87-899a-db2af29f87da"},{"op":"add","path":"/memberId","value":"16a7c47a-11df-46d1-a7c4-7a11dfe6d135"}]',
      },
      {
        id: '54397606-3ce9-4845-b976-063ce9284553',
        referenceId: '9c899adb-2af2-4f87-899a-db2af29f87da',
        referenceType: 'ENVIRONMENT',
        user: 'system',
        createdAt: 1650364807532,
        event: 'MEMBERSHIP_DELETED',
        properties: {
          USER: '16a7c47a-11df-46d1-a7c4-7a11dfe6d135',
        },
        patch:
          '[{"op":"remove","path":"/roleId"},{"op":"remove","path":"/referenceType"},{"op":"remove","path":"/id"},{"op":"remove","path":"/memberType"},{"op":"remove","path":"/source"},{"op":"remove","path":"/referenceId"},{"op":"remove","path":"/memberId"}]',
      },
      {
        id: '24c9fa6a-45a7-4000-89fa-6a45a72000c1',
        referenceId: '9c899adb-2af2-4f87-899a-db2af29f87da',
        referenceType: 'ENVIRONMENT',
        user: 'system',
        createdAt: 1650364807514,
        event: 'MEMBERSHIP_CREATED',
        properties: {
          USER: '985d9fac-e9ea-447f-9d9f-ace9ea747fb9',
        },
        patch:
          '[{"op":"add","path":"/roleId","value":"4c40b33d-6778-40bc-80b3-3d677860bc2a"},{"op":"add","path":"/referenceType","value":"ENVIRONMENT"},{"op":"add","path":"/id","value":"5aa579eb-63ed-4e92-a579-eb63ed4e9241"},{"op":"add","path":"/memberType","value":"USER"},{"op":"add","path":"/source","value":"cockpit"},{"op":"add","path":"/referenceId","value":"9c899adb-2af2-4f87-899a-db2af29f87da"},{"op":"add","path":"/memberId","value":"985d9fac-e9ea-447f-9d9f-ace9ea747fb9"}]',
      },
      {
        id: 'c6c1b207-504d-49af-81b2-07504d99aff2',
        referenceId: '9c899adb-2af2-4f87-899a-db2af29f87da',
        referenceType: 'ENVIRONMENT',
        user: 'system',
        createdAt: 1650364807509,
        event: 'MEMBERSHIP_DELETED',
        properties: {
          USER: '985d9fac-e9ea-447f-9d9f-ace9ea747fb9',
        },
        patch:
          '[{"op":"remove","path":"/roleId"},{"op":"remove","path":"/referenceType"},{"op":"remove","path":"/id"},{"op":"remove","path":"/memberType"},{"op":"remove","path":"/source"},{"op":"remove","path":"/referenceId"},{"op":"remove","path":"/memberId"}]',
      },
      {
        id: '64263490-e5a9-4846-a634-90e5a9c846ae',
        referenceId: '9c899adb-2af2-4f87-899a-db2af29f87da',
        referenceType: 'ENVIRONMENT',
        user: 'system',
        createdAt: 1650364807488,
        event: 'MEMBERSHIP_CREATED',
        properties: {
          USER: '37c5da71-7db3-45ce-85da-717db305ce8c',
        },
        patch:
          '[{"op":"add","path":"/roleId","value":"4c40b33d-6778-40bc-80b3-3d677860bc2a"},{"op":"add","path":"/referenceType","value":"ENVIRONMENT"},{"op":"add","path":"/id","value":"92b629e0-258e-48ac-b629-e0258ec8acda"},{"op":"add","path":"/memberType","value":"USER"},{"op":"add","path":"/source","value":"cockpit"},{"op":"add","path":"/referenceId","value":"9c899adb-2af2-4f87-899a-db2af29f87da"},{"op":"add","path":"/memberId","value":"37c5da71-7db3-45ce-85da-717db305ce8c"}]',
      },
    ],
    metadata: {
      'ROLE:ORGANIZATION:ADMIN:name': 'ORGANIZATION:ADMIN',
      'METADATA:sub:name': 'sub',
      'USER:system:name': 'system',
      'METADATA:picture:name': 'picture',
      'METADATA:name:name': 'name',
      'METADATA:family_name:name': 'family_name',
      'THEME:default:name': 'default',
      'USER:fb8b0c31-5428-4a4c-8b0c-315428da4c77:name': 'Thibaud \uD83E\uDD8A',
      'METADATA:email:name': 'email',
      'METADATA:given_name:name': 'given_name',
      'METADATA:preferred_username:name': 'preferred_username',
      'USER:985d9fac-e9ea-447f-9d9f-ace9ea747fb9:name': 'Florent CHAMFROY',
      'USER:37c5da71-7db3-45ce-85da-717db305ce8c:name': 'Yann Tavernier',
      'USER:16a7c47a-11df-46d1-a7c4-7a11dfe6d135:name': 'Ruben Santos',
      'USER:f8f23327-30a2-4b51-b233-2730a27b5195:name': 'Lilia Enachi',
    },
    pageElements: 1,
    totalElements: 1516,
    pageNumber: 1,
  };

  return {
    ...base,
    ...attributes,
  };
}
