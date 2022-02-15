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
import { ADMIN_USER, API_PUBLISHER_USER, APPLICATION_USER, LOW_PERMISSION_USER } from '@fakers/users/users';
import { ApiFakers } from '@fakers/apis';
import { Api, ApiErrorCodes, ApiLifecycleState, ApiMember, ApiVisibility, PortalApi } from '@model/apis';
import { CollectionResponse, PortalError } from '@model/technical';
import { User } from '@model/users';
import { ApiAssertions, PortalApiAssertions } from 'assertions/api.assertion';
import { ErrorAssertions } from 'assertions/error.assertion';
import { gio } from '@commands/gravitee.commands';

context('API - Visibility', () => {
  describe('Private', () => {
    let createdApi: Api;
    let lowPermissionUser: User;

    describe('Prepare', () => {
      it('Should create an API', () => {
        const fakeApi: Api = ApiFakers.api();
        gio
          .management(API_PUBLISHER_USER)
          .apis()
          .create(fakeApi)
          .created()
          .should((response) => {
            ApiAssertions.assertThat(response).hasBeenCreated(fakeApi);
          })
          .then((response) => {
            createdApi = response.body;
            cy.log('Created api id:', createdApi.id);
          });
      });

      it('Should publish the API', () => {
        const apiToPublish = {
          ...createdApi,
          lifecycle_state: ApiLifecycleState.PUBLISHED,
        };
        delete apiToPublish.id;
        delete apiToPublish.state;
        delete apiToPublish.created_at;
        delete apiToPublish.updated_at;
        delete apiToPublish.owner;
        delete apiToPublish.contextPath;

        gio
          .management(API_PUBLISHER_USER)
          .apis()
          .update(createdApi.id, apiToPublish)
          .ok()
          .should((response) => {
            ApiAssertions.assertThat(response).hasBeenPublished(apiToPublish);
          })
          .then((response) => {
            createdApi = response.body;
            cy.log('Created api id:', createdApi.id);
          });
      });

      it('Should get LOW_PERMISSION user', () => {
        gio
          .management(API_PUBLISHER_USER)
          .users()
          .searchUser('user')
          .ok()
          .should((response) => {
            const users = response.body;
            const lowPermUserFromResponse = users.find((u) => u.displayName === 'user');

            expect(lowPermUserFromResponse).to.be.exist;
            lowPermissionUser = lowPermUserFromResponse;
          });
      });

      it('Should add LOW_PERMISSION user as member of the API', () => {
        const memberToAdd: ApiMember = {
          ...lowPermissionUser,
          role: 'USER',
        };
        gio.management(API_PUBLISHER_USER).apis().addMemberToApi(createdApi.id, memberToAdd).created();
      });
    });

    describe('As ANONYMOUS user', () => {
      it('Get APIs should not contain created api', () => {
        gio
          .portal()
          .apis()
          .getAll<CollectionResponse<PortalApi>>()
          .ok()
          .should((response) => {
            let apiIds = response.body.data.map((api) => api.id);
            expect(apiIds).to.not.contain(createdApi.id);
          });
      });

      it('Get API should return 404 - Not Found', () => {
        gio
          .portal()
          .apis()
          .getApiById<PortalError>(createdApi.id)
          .notFound()
          .should((response) => {
            ErrorAssertions.assertThat(response).containsMessage(createdApi.id).containsCode(ApiErrorCodes.API_NOT_FOUND);
          });
      });
    });

    describe('As ADMIN user', () => {
      it('Get APIs should not contain created api', () => {
        gio
          .portal(ADMIN_USER)
          .apis()
          .getAll<CollectionResponse<PortalApi>>()
          .ok()
          .should((response) => {
            let apiIds = response.body.data.map((api) => api.id);
            expect(apiIds).to.not.contain(createdApi.id);
          });
      });

      it('Get API should return 404 - Not Found', () => {
        gio
          .portal(ADMIN_USER)
          .apis()
          .getApiById<PortalError>(createdApi.id)
          .notFound()
          .should((response) => {
            ErrorAssertions.assertThat(response).containsMessage(createdApi.id).containsCode(ApiErrorCodes.API_NOT_FOUND);
          });
      });
    });

    describe('As API_PUBLISHER user', () => {
      it('Get APIs should contain created api', () => {
        gio
          .portal(API_PUBLISHER_USER)
          .apis()
          .getAll<CollectionResponse<PortalApi>>()
          .ok()
          .should((response) => {
            let apiIds = response.body.data.map((api) => api.id);
            expect(apiIds).to.contain(createdApi.id);
          });
      });

      it('Should get API', () => {
        gio
          .portal(API_PUBLISHER_USER)
          .apis()
          .getApiById<PortalApi>(createdApi.id)
          .ok()
          .should((response) => {
            PortalApiAssertions.assertThat(response).hasId(createdApi.id).isNotRunning().isNotPublic().isNotDraft();
          });
      });
    });

    describe('As APPLICATION user', () => {
      it('Get APIs should not contain created api', () => {
        gio
          .portal(APPLICATION_USER)
          .apis()
          .getAll<CollectionResponse<PortalApi>>()
          .ok()
          .should((response) => {
            let apiIds = response.body.data.map((api) => api.id);
            expect(apiIds).to.not.contain(createdApi.id);
          });
      });

      it('Get API should return 404 - Not Found', () => {
        gio
          .portal(APPLICATION_USER)
          .apis()
          .getApiById<PortalError>(createdApi.id)
          .notFound()
          .should((response) => {
            ErrorAssertions.assertThat(response).containsMessage(createdApi.id).containsCode(ApiErrorCodes.API_NOT_FOUND);
          });
      });
    });

    describe('As LOW_PERMISSION user', () => {
      it('Get APIs should contain created api', () => {
        gio
          .portal(LOW_PERMISSION_USER)
          .apis()
          .getAll<CollectionResponse<PortalApi>>()
          .ok()
          .should((response) => {
            let apiIds = response.body.data.map((api) => api.id);
            expect(apiIds).to.contain(createdApi.id);
          });
      });

      it('Should get API', () => {
        gio
          .portal(LOW_PERMISSION_USER)
          .apis()
          .getApiById<PortalApi>(createdApi.id)
          .ok()
          .should((response) => {
            PortalApiAssertions.assertThat(response).hasId(createdApi.id).isNotRunning().isNotPublic().isNotDraft();
          });
      });
    });

    describe('Clean up', () => {
      it('Should delete the API', () => {
        gio.management(ADMIN_USER).apis().delete(createdApi.id).noContent();
      });
    });
  });

  describe('Public', () => {
    let createdApi: Api;
    let lowPermissionUser: User;

    describe('Prepare', () => {
      it('Should create an API', () => {
        const fakeApi: Api = ApiFakers.api();
        gio
          .management(API_PUBLISHER_USER)
          .apis()
          .create(fakeApi)
          .created()
          .should((response) => {
            ApiAssertions.assertThat(response).hasBeenCreated(fakeApi);
          })
          .then((response) => {
            createdApi = response.body;
            cy.log('Created api id:', createdApi.id);
          });
      });

      it('Should publish the API', () => {
        const apiToPublish = {
          ...createdApi,
          lifecycle_state: ApiLifecycleState.PUBLISHED,
        };
        delete apiToPublish.id;
        delete apiToPublish.state;
        delete apiToPublish.created_at;
        delete apiToPublish.updated_at;
        delete apiToPublish.owner;
        delete apiToPublish.contextPath;

        gio
          .management(API_PUBLISHER_USER)
          .apis()
          .update(createdApi.id, apiToPublish)
          .ok()
          .should((response) => {
            ApiAssertions.assertThat(response).hasBeenPublished(apiToPublish);
          })
          .then((response) => {
            createdApi = response.body;
            cy.log('Created api id:', createdApi.id);
          });
      });

      it('Should get LOW_PERMISSION user', () => {
        gio
          .management(API_PUBLISHER_USER)
          .users()
          .searchUser('user')
          .ok()
          .should((response) => {
            const users = response.body;
            const lowPermUserFromResponse = users.find((u) => u.displayName === 'user');

            expect(lowPermUserFromResponse).to.be.exist;
            lowPermissionUser = lowPermUserFromResponse;
          });
      });

      it('Should add LOW_PERMISSION user as member of the API', () => {
        const memberToAdd: ApiMember = {
          ...lowPermissionUser,
          role: 'USER',
        };
        gio.management(API_PUBLISHER_USER).apis().addMemberToApi(createdApi.id, memberToAdd).created();
      });

      it('Should make the API public', () => {
        const apiToMakePublic: Api = {
          ...createdApi,
          visibility: ApiVisibility.PUBLIC,
        };
        delete apiToMakePublic.id;
        delete apiToMakePublic.state;
        delete apiToMakePublic.created_at;
        delete apiToMakePublic.updated_at;
        delete apiToMakePublic.owner;
        delete apiToMakePublic.contextPath;

        gio
          .management(API_PUBLISHER_USER)
          .apis()
          .update(createdApi.id, apiToMakePublic)
          .ok()
          .should((response) => {
            ApiAssertions.assertThat(response).hasId(apiToMakePublic.id).hasVisibility(ApiVisibility.PUBLIC);
          })
          .then((response) => {
            createdApi = response.body;
          });
      });
    });

    describe('As ANONYMOUS user', () => {
      it('Get APIs should contain created api', () => {
        gio
          .portal(API_PUBLISHER_USER)
          .apis()
          .getAll<CollectionResponse<PortalApi>>()
          .ok()
          .should((response) => {
            let apiIds = response.body.data.map((api) => api.id);
            expect(apiIds).to.contain(createdApi.id);
          });
      });

      it('Should get API', () => {
        gio
          .portal(API_PUBLISHER_USER)
          .apis()
          .getApiById<PortalApi>(createdApi.id)
          .ok()
          .should((response) => {
            PortalApiAssertions.assertThat(response).hasId(createdApi.id).isNotRunning().isPublic().isNotDraft();
          });
      });
    });

    describe('As ADMIN user', () => {
      it('Get APIs should contain created api', () => {
        gio
          .portal(API_PUBLISHER_USER)
          .apis()
          .getAll<CollectionResponse<PortalApi>>()
          .ok()
          .should((response) => {
            let apiIds = response.body.data.map((api) => api.id);
            expect(apiIds).to.contain(createdApi.id);
          });
      });

      it('Should get API', () => {
        gio
          .portal(API_PUBLISHER_USER)
          .apis()
          .getApiById<PortalApi>(createdApi.id)
          .ok()
          .should((response) => {
            PortalApiAssertions.assertThat(response).hasId(createdApi.id).isNotRunning().isPublic().isNotDraft();
          });
      });
    });

    describe('As API_PUBLISHER user', () => {
      it('Get APIs should contain created api', () => {
        gio
          .portal(API_PUBLISHER_USER)
          .apis()
          .getAll<CollectionResponse<PortalApi>>()
          .ok()
          .should((response) => {
            let apiIds = response.body.data.map((api) => api.id);
            expect(apiIds).to.contain(createdApi.id);
          });
      });

      it('Should get API', () => {
        gio
          .portal(API_PUBLISHER_USER)
          .apis()
          .getApiById<PortalApi>(createdApi.id)
          .ok()
          .should((response) => {
            PortalApiAssertions.assertThat(response).hasId(createdApi.id).isNotRunning().isPublic().isNotDraft();
          });
      });
    });

    describe('As APPLICATION user', () => {
      it('Get APIs should contain created api', () => {
        gio
          .portal(API_PUBLISHER_USER)
          .apis()
          .getAll<CollectionResponse<PortalApi>>()
          .ok()
          .should((response) => {
            let apiIds = response.body.data.map((api) => api.id);
            expect(apiIds).to.contain(createdApi.id);
          });
      });

      it('Should get API', () => {
        gio
          .portal(API_PUBLISHER_USER)
          .apis()
          .getApiById<PortalApi>(createdApi.id)
          .ok()
          .should((response) => {
            PortalApiAssertions.assertThat(response).hasId(createdApi.id).isNotRunning().isPublic().isNotDraft();
          });
      });
    });

    describe('As LOW_PERMISSION user', () => {
      it('Get APIs should contain created api', () => {
        gio
          .portal(LOW_PERMISSION_USER)
          .apis()
          .getAll<CollectionResponse<PortalApi>>()
          .ok()
          .should((response) => {
            let apiIds = response.body.data.map((api) => api.id);
            expect(apiIds).to.contain(createdApi.id);
          });
      });

      it('Should get API', () => {
        gio
          .portal(LOW_PERMISSION_USER)
          .apis()
          .getApiById<PortalApi>(createdApi.id)
          .ok()
          .should((response) => {
            PortalApiAssertions.assertThat(response).hasId(createdApi.id).isNotRunning().isPublic().isNotDraft();
          });
      });
    });

    describe('Clean up', () => {
      it('Should delete the API', () => {
        gio.management(ADMIN_USER).apis().delete(createdApi.id).noContent();
      });
    });
  });
});
