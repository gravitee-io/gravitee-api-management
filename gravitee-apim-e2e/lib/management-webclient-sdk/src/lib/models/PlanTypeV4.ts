/* tslint:disable */
/* eslint-disable */
/**
 * Gravitee.io - Management API
 * No description provided (generated by Openapi Generator https://github.com/openapitools/openapi-generator)
 *
 * 
 *
 * NOTE: This class is auto generated by OpenAPI Generator (https://openapi-generator.tech).
 * https://openapi-generator.tech
 * Do not edit the class manually.
 */

/**
 * 
 * @export
 * @enum {string}
 */
export enum PlanTypeV4 {
    API = 'API',
    CATALOG = 'CATALOG'
}

export function PlanTypeV4FromJSON(json: any): PlanTypeV4 {
    return PlanTypeV4FromJSONTyped(json, false);
}

export function PlanTypeV4FromJSONTyped(json: any, ignoreDiscriminator: boolean): PlanTypeV4 {
    return json as PlanTypeV4;
}

export function PlanTypeV4ToJSON(value?: PlanTypeV4 | null): any {
    return value as any;
}

