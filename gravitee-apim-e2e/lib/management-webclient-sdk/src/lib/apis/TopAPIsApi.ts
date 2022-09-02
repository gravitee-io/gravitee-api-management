/* tslint:disable */
/* eslint-disable */
/**
 * Gravitee.io - Management API
 * Some news resources are in alpha version. This implies that they are likely to be modified or even removed in future versions. They are marked with the 🧪 symbol
 *
 * 
 *
 * NOTE: This class is auto generated by OpenAPI Generator (https://openapi-generator.tech).
 * https://openapi-generator.tech
 * Do not edit the class manually.
 */


import * as runtime from '../runtime';
import type {
  NewTopApiEntity,
  TopApiEntity,
  UpdateTopApiEntity,
} from '../models';
import {
    NewTopApiEntityFromJSON,
    NewTopApiEntityToJSON,
    TopApiEntityFromJSON,
    TopApiEntityToJSON,
    UpdateTopApiEntityFromJSON,
    UpdateTopApiEntityToJSON,
} from '../models';

export interface CreateTopApiRequest {
    envId: string;
    orgId: string;
    newTopApiEntity: NewTopApiEntity;
}

export interface DeleteTopApiRequest {
    topAPI: string;
    envId: string;
    orgId: string;
}

export interface GetTopApisRequest {
    envId: string;
    orgId: string;
}

export interface UpdateTopApiRequest {
    envId: string;
    orgId: string;
    updateTopApiEntity: Array<UpdateTopApiEntity>;
}

/**
 * 
 */
export class TopAPIsApi extends runtime.BaseAPI {

    /**
     * User must have the PORTAL_TOP_APIS[CREATE] permission to use this service
     * Create a top API
     */
    async createTopApiRaw(requestParameters: CreateTopApiRequest, initOverrides?: RequestInit | runtime.InitOverrideFunction): Promise<runtime.ApiResponse<Array<TopApiEntity>>> {
        if (requestParameters.envId === null || requestParameters.envId === undefined) {
            throw new runtime.RequiredError('envId','Required parameter requestParameters.envId was null or undefined when calling createTopApi.');
        }

        if (requestParameters.orgId === null || requestParameters.orgId === undefined) {
            throw new runtime.RequiredError('orgId','Required parameter requestParameters.orgId was null or undefined when calling createTopApi.');
        }

        if (requestParameters.newTopApiEntity === null || requestParameters.newTopApiEntity === undefined) {
            throw new runtime.RequiredError('newTopApiEntity','Required parameter requestParameters.newTopApiEntity was null or undefined when calling createTopApi.');
        }

        const queryParameters: any = {};

        const headerParameters: runtime.HTTPHeaders = {};

        headerParameters['Content-Type'] = 'application/json';

        if (this.configuration && (this.configuration.username !== undefined || this.configuration.password !== undefined)) {
            headerParameters["Authorization"] = "Basic " + btoa(this.configuration.username + ":" + this.configuration.password);
        }
        const response = await this.request({
            path: `/organizations/{orgId}/environments/{envId}/configuration/top-apis`.replace(`{${"envId"}}`, encodeURIComponent(String(requestParameters.envId))).replace(`{${"orgId"}}`, encodeURIComponent(String(requestParameters.orgId))),
            method: 'POST',
            headers: headerParameters,
            query: queryParameters,
            body: NewTopApiEntityToJSON(requestParameters.newTopApiEntity),
        }, initOverrides);

        return new runtime.JSONApiResponse(response, (jsonValue) => jsonValue.map(TopApiEntityFromJSON));
    }

    /**
     * User must have the PORTAL_TOP_APIS[CREATE] permission to use this service
     * Create a top API
     */
    async createTopApi(requestParameters: CreateTopApiRequest, initOverrides?: RequestInit | runtime.InitOverrideFunction): Promise<Array<TopApiEntity>> {
        const response = await this.createTopApiRaw(requestParameters, initOverrides);
        return await response.value();
    }

    /**
     * User must have the PORTAL_TOP_APIS[DELETE] permission to use this service
     * Delete an existing top API
     */
    async deleteTopApiRaw(requestParameters: DeleteTopApiRequest, initOverrides?: RequestInit | runtime.InitOverrideFunction): Promise<runtime.ApiResponse<void>> {
        if (requestParameters.topAPI === null || requestParameters.topAPI === undefined) {
            throw new runtime.RequiredError('topAPI','Required parameter requestParameters.topAPI was null or undefined when calling deleteTopApi.');
        }

        if (requestParameters.envId === null || requestParameters.envId === undefined) {
            throw new runtime.RequiredError('envId','Required parameter requestParameters.envId was null or undefined when calling deleteTopApi.');
        }

        if (requestParameters.orgId === null || requestParameters.orgId === undefined) {
            throw new runtime.RequiredError('orgId','Required parameter requestParameters.orgId was null or undefined when calling deleteTopApi.');
        }

        const queryParameters: any = {};

        const headerParameters: runtime.HTTPHeaders = {};

        if (this.configuration && (this.configuration.username !== undefined || this.configuration.password !== undefined)) {
            headerParameters["Authorization"] = "Basic " + btoa(this.configuration.username + ":" + this.configuration.password);
        }
        const response = await this.request({
            path: `/organizations/{orgId}/environments/{envId}/configuration/top-apis/{topAPI}`.replace(`{${"topAPI"}}`, encodeURIComponent(String(requestParameters.topAPI))).replace(`{${"envId"}}`, encodeURIComponent(String(requestParameters.envId))).replace(`{${"orgId"}}`, encodeURIComponent(String(requestParameters.orgId))),
            method: 'DELETE',
            headers: headerParameters,
            query: queryParameters,
        }, initOverrides);

        return new runtime.VoidApiResponse(response);
    }

    /**
     * User must have the PORTAL_TOP_APIS[DELETE] permission to use this service
     * Delete an existing top API
     */
    async deleteTopApi(requestParameters: DeleteTopApiRequest, initOverrides?: RequestInit | runtime.InitOverrideFunction): Promise<void> {
        await this.deleteTopApiRaw(requestParameters, initOverrides);
    }

    /**
     * User must have the PORTAL_TOP_APIS[READ] permission to use this service
     * List of top APIs
     */
    async getTopApisRaw(requestParameters: GetTopApisRequest, initOverrides?: RequestInit | runtime.InitOverrideFunction): Promise<runtime.ApiResponse<Array<TopApiEntity>>> {
        if (requestParameters.envId === null || requestParameters.envId === undefined) {
            throw new runtime.RequiredError('envId','Required parameter requestParameters.envId was null or undefined when calling getTopApis.');
        }

        if (requestParameters.orgId === null || requestParameters.orgId === undefined) {
            throw new runtime.RequiredError('orgId','Required parameter requestParameters.orgId was null or undefined when calling getTopApis.');
        }

        const queryParameters: any = {};

        const headerParameters: runtime.HTTPHeaders = {};

        if (this.configuration && (this.configuration.username !== undefined || this.configuration.password !== undefined)) {
            headerParameters["Authorization"] = "Basic " + btoa(this.configuration.username + ":" + this.configuration.password);
        }
        const response = await this.request({
            path: `/organizations/{orgId}/environments/{envId}/configuration/top-apis`.replace(`{${"envId"}}`, encodeURIComponent(String(requestParameters.envId))).replace(`{${"orgId"}}`, encodeURIComponent(String(requestParameters.orgId))),
            method: 'GET',
            headers: headerParameters,
            query: queryParameters,
        }, initOverrides);

        return new runtime.JSONApiResponse(response, (jsonValue) => jsonValue.map(TopApiEntityFromJSON));
    }

    /**
     * User must have the PORTAL_TOP_APIS[READ] permission to use this service
     * List of top APIs
     */
    async getTopApis(requestParameters: GetTopApisRequest, initOverrides?: RequestInit | runtime.InitOverrideFunction): Promise<Array<TopApiEntity>> {
        const response = await this.getTopApisRaw(requestParameters, initOverrides);
        return await response.value();
    }

    /**
     * User must have the PORTAL_TOP_APIS[UPDATE] permission to use this service
     * Update a top API
     */
    async updateTopApiRaw(requestParameters: UpdateTopApiRequest, initOverrides?: RequestInit | runtime.InitOverrideFunction): Promise<runtime.ApiResponse<Array<TopApiEntity>>> {
        if (requestParameters.envId === null || requestParameters.envId === undefined) {
            throw new runtime.RequiredError('envId','Required parameter requestParameters.envId was null or undefined when calling updateTopApi.');
        }

        if (requestParameters.orgId === null || requestParameters.orgId === undefined) {
            throw new runtime.RequiredError('orgId','Required parameter requestParameters.orgId was null or undefined when calling updateTopApi.');
        }

        if (requestParameters.updateTopApiEntity === null || requestParameters.updateTopApiEntity === undefined) {
            throw new runtime.RequiredError('updateTopApiEntity','Required parameter requestParameters.updateTopApiEntity was null or undefined when calling updateTopApi.');
        }

        const queryParameters: any = {};

        const headerParameters: runtime.HTTPHeaders = {};

        headerParameters['Content-Type'] = 'application/json';

        if (this.configuration && (this.configuration.username !== undefined || this.configuration.password !== undefined)) {
            headerParameters["Authorization"] = "Basic " + btoa(this.configuration.username + ":" + this.configuration.password);
        }
        const response = await this.request({
            path: `/organizations/{orgId}/environments/{envId}/configuration/top-apis`.replace(`{${"envId"}}`, encodeURIComponent(String(requestParameters.envId))).replace(`{${"orgId"}}`, encodeURIComponent(String(requestParameters.orgId))),
            method: 'PUT',
            headers: headerParameters,
            query: queryParameters,
            body: requestParameters.updateTopApiEntity.map(UpdateTopApiEntityToJSON),
        }, initOverrides);

        return new runtime.JSONApiResponse(response, (jsonValue) => jsonValue.map(TopApiEntityFromJSON));
    }

    /**
     * User must have the PORTAL_TOP_APIS[UPDATE] permission to use this service
     * Update a top API
     */
    async updateTopApi(requestParameters: UpdateTopApiRequest, initOverrides?: RequestInit | runtime.InitOverrideFunction): Promise<Array<TopApiEntity>> {
        const response = await this.updateTopApiRaw(requestParameters, initOverrides);
        return await response.value();
    }

}
