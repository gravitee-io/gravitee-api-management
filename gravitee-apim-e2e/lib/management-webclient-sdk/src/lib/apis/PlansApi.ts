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
  PlansConfigurationEntity,
} from '../models';
import {
    PlansConfigurationEntityFromJSON,
    PlansConfigurationEntityToJSON,
} from '../models';

export interface GetPlansConfigurationRequest {
    envId: string;
    orgId: string;
}

/**
 * 
 */
export class PlansApi extends runtime.BaseAPI {

    /**
     * List of available plan\'s type
     */
    async getPlansConfigurationRaw(requestParameters: GetPlansConfigurationRequest, initOverrides?: RequestInit | runtime.InitOverrideFunction): Promise<runtime.ApiResponse<PlansConfigurationEntity>> {
        if (requestParameters.envId === null || requestParameters.envId === undefined) {
            throw new runtime.RequiredError('envId','Required parameter requestParameters.envId was null or undefined when calling getPlansConfiguration.');
        }

        if (requestParameters.orgId === null || requestParameters.orgId === undefined) {
            throw new runtime.RequiredError('orgId','Required parameter requestParameters.orgId was null or undefined when calling getPlansConfiguration.');
        }

        const queryParameters: any = {};

        const headerParameters: runtime.HTTPHeaders = {};

        if (this.configuration && (this.configuration.username !== undefined || this.configuration.password !== undefined)) {
            headerParameters["Authorization"] = "Basic " + btoa(this.configuration.username + ":" + this.configuration.password);
        }
        const response = await this.request({
            path: `/organizations/{orgId}/environments/{envId}/configuration/plans`.replace(`{${"envId"}}`, encodeURIComponent(String(requestParameters.envId))).replace(`{${"orgId"}}`, encodeURIComponent(String(requestParameters.orgId))),
            method: 'GET',
            headers: headerParameters,
            query: queryParameters,
        }, initOverrides);

        return new runtime.JSONApiResponse(response, (jsonValue) => PlansConfigurationEntityFromJSON(jsonValue));
    }

    /**
     * List of available plan\'s type
     */
    async getPlansConfiguration(requestParameters: GetPlansConfigurationRequest, initOverrides?: RequestInit | runtime.InitOverrideFunction): Promise<PlansConfigurationEntity> {
        const response = await this.getPlansConfigurationRaw(requestParameters, initOverrides);
        return await response.value();
    }

}
