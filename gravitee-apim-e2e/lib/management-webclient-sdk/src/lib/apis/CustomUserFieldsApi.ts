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
  CustomUserFieldEntity,
} from '../models';
import {
    CustomUserFieldEntityFromJSON,
    CustomUserFieldEntityToJSON,
} from '../models';

export interface CreateCustomUserFieldRequest {
    orgId: string;
    customUserFieldEntity?: CustomUserFieldEntity;
}

export interface DeleteCustomUserFieldRequest {
    key: string;
    orgId: string;
}

export interface GetCustomUserFieldsRequest {
    orgId: string;
}

export interface UpdateCustomUserFieldRequest {
    key: string;
    orgId: string;
    customUserFieldEntity?: CustomUserFieldEntity;
}

/**
 * 
 */
export class CustomUserFieldsApi extends runtime.BaseAPI {

    /**
     * User must have the CUSTOM_USER_FIELDS[CREATE] permission to use this service
     * Create a Custom User Field
     */
    async createCustomUserFieldRaw(requestParameters: CreateCustomUserFieldRequest, initOverrides?: RequestInit | runtime.InitOverrideFunction): Promise<runtime.ApiResponse<CustomUserFieldEntity>> {
        if (requestParameters.orgId === null || requestParameters.orgId === undefined) {
            throw new runtime.RequiredError('orgId','Required parameter requestParameters.orgId was null or undefined when calling createCustomUserField.');
        }

        const queryParameters: any = {};

        const headerParameters: runtime.HTTPHeaders = {};

        headerParameters['Content-Type'] = 'application/json';

        if (this.configuration && (this.configuration.username !== undefined || this.configuration.password !== undefined)) {
            headerParameters["Authorization"] = "Basic " + btoa(this.configuration.username + ":" + this.configuration.password);
        }
        const response = await this.request({
            path: `/organizations/{orgId}/configuration/custom-user-fields`.replace(`{${"orgId"}}`, encodeURIComponent(String(requestParameters.orgId))),
            method: 'POST',
            headers: headerParameters,
            query: queryParameters,
            body: CustomUserFieldEntityToJSON(requestParameters.customUserFieldEntity),
        }, initOverrides);

        return new runtime.JSONApiResponse(response, (jsonValue) => CustomUserFieldEntityFromJSON(jsonValue));
    }

    /**
     * User must have the CUSTOM_USER_FIELDS[CREATE] permission to use this service
     * Create a Custom User Field
     */
    async createCustomUserField(requestParameters: CreateCustomUserFieldRequest, initOverrides?: RequestInit | runtime.InitOverrideFunction): Promise<CustomUserFieldEntity> {
        const response = await this.createCustomUserFieldRaw(requestParameters, initOverrides);
        return await response.value();
    }

    /**
     * User must have the CUSTOM_USER_FIELDS[DELETE] permission to use this service
     * Delete a Custom User Field
     */
    async deleteCustomUserFieldRaw(requestParameters: DeleteCustomUserFieldRequest, initOverrides?: RequestInit | runtime.InitOverrideFunction): Promise<runtime.ApiResponse<void>> {
        if (requestParameters.key === null || requestParameters.key === undefined) {
            throw new runtime.RequiredError('key','Required parameter requestParameters.key was null or undefined when calling deleteCustomUserField.');
        }

        if (requestParameters.orgId === null || requestParameters.orgId === undefined) {
            throw new runtime.RequiredError('orgId','Required parameter requestParameters.orgId was null or undefined when calling deleteCustomUserField.');
        }

        const queryParameters: any = {};

        const headerParameters: runtime.HTTPHeaders = {};

        if (this.configuration && (this.configuration.username !== undefined || this.configuration.password !== undefined)) {
            headerParameters["Authorization"] = "Basic " + btoa(this.configuration.username + ":" + this.configuration.password);
        }
        const response = await this.request({
            path: `/organizations/{orgId}/configuration/custom-user-fields/{key}`.replace(`{${"key"}}`, encodeURIComponent(String(requestParameters.key))).replace(`{${"orgId"}}`, encodeURIComponent(String(requestParameters.orgId))),
            method: 'DELETE',
            headers: headerParameters,
            query: queryParameters,
        }, initOverrides);

        return new runtime.VoidApiResponse(response);
    }

    /**
     * User must have the CUSTOM_USER_FIELDS[DELETE] permission to use this service
     * Delete a Custom User Field
     */
    async deleteCustomUserField(requestParameters: DeleteCustomUserFieldRequest, initOverrides?: RequestInit | runtime.InitOverrideFunction): Promise<void> {
        await this.deleteCustomUserFieldRaw(requestParameters, initOverrides);
    }

    /**
     * User must have the CUSTOM_USER_FIELDS[READ] permission to use this service
     * List All Custom User Fields
     */
    async getCustomUserFieldsRaw(requestParameters: GetCustomUserFieldsRequest, initOverrides?: RequestInit | runtime.InitOverrideFunction): Promise<runtime.ApiResponse<Array<CustomUserFieldEntity>>> {
        if (requestParameters.orgId === null || requestParameters.orgId === undefined) {
            throw new runtime.RequiredError('orgId','Required parameter requestParameters.orgId was null or undefined when calling getCustomUserFields.');
        }

        const queryParameters: any = {};

        const headerParameters: runtime.HTTPHeaders = {};

        if (this.configuration && (this.configuration.username !== undefined || this.configuration.password !== undefined)) {
            headerParameters["Authorization"] = "Basic " + btoa(this.configuration.username + ":" + this.configuration.password);
        }
        const response = await this.request({
            path: `/organizations/{orgId}/configuration/custom-user-fields`.replace(`{${"orgId"}}`, encodeURIComponent(String(requestParameters.orgId))),
            method: 'GET',
            headers: headerParameters,
            query: queryParameters,
        }, initOverrides);

        return new runtime.JSONApiResponse(response, (jsonValue) => jsonValue.map(CustomUserFieldEntityFromJSON));
    }

    /**
     * User must have the CUSTOM_USER_FIELDS[READ] permission to use this service
     * List All Custom User Fields
     */
    async getCustomUserFields(requestParameters: GetCustomUserFieldsRequest, initOverrides?: RequestInit | runtime.InitOverrideFunction): Promise<Array<CustomUserFieldEntity>> {
        const response = await this.getCustomUserFieldsRaw(requestParameters, initOverrides);
        return await response.value();
    }

    /**
     * User must have the CUSTOM_USER_FIELDS[UPDATE] permission to use this service
     * Update a Custom User Field
     */
    async updateCustomUserFieldRaw(requestParameters: UpdateCustomUserFieldRequest, initOverrides?: RequestInit | runtime.InitOverrideFunction): Promise<runtime.ApiResponse<CustomUserFieldEntity>> {
        if (requestParameters.key === null || requestParameters.key === undefined) {
            throw new runtime.RequiredError('key','Required parameter requestParameters.key was null or undefined when calling updateCustomUserField.');
        }

        if (requestParameters.orgId === null || requestParameters.orgId === undefined) {
            throw new runtime.RequiredError('orgId','Required parameter requestParameters.orgId was null or undefined when calling updateCustomUserField.');
        }

        const queryParameters: any = {};

        const headerParameters: runtime.HTTPHeaders = {};

        headerParameters['Content-Type'] = 'application/json';

        if (this.configuration && (this.configuration.username !== undefined || this.configuration.password !== undefined)) {
            headerParameters["Authorization"] = "Basic " + btoa(this.configuration.username + ":" + this.configuration.password);
        }
        const response = await this.request({
            path: `/organizations/{orgId}/configuration/custom-user-fields/{key}`.replace(`{${"key"}}`, encodeURIComponent(String(requestParameters.key))).replace(`{${"orgId"}}`, encodeURIComponent(String(requestParameters.orgId))),
            method: 'PUT',
            headers: headerParameters,
            query: queryParameters,
            body: CustomUserFieldEntityToJSON(requestParameters.customUserFieldEntity),
        }, initOverrides);

        return new runtime.JSONApiResponse(response, (jsonValue) => CustomUserFieldEntityFromJSON(jsonValue));
    }

    /**
     * User must have the CUSTOM_USER_FIELDS[UPDATE] permission to use this service
     * Update a Custom User Field
     */
    async updateCustomUserField(requestParameters: UpdateCustomUserFieldRequest, initOverrides?: RequestInit | runtime.InitOverrideFunction): Promise<CustomUserFieldEntity> {
        const response = await this.updateCustomUserFieldRaw(requestParameters, initOverrides);
        return await response.value();
    }

}
