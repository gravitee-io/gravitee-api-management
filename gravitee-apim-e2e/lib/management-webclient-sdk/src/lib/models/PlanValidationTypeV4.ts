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
export enum PlanValidationTypeV4 {
    AUTO = 'AUTO',
    MANUAL = 'MANUAL'
}

export function PlanValidationTypeV4FromJSON(json: any): PlanValidationTypeV4 {
    return PlanValidationTypeV4FromJSONTyped(json, false);
}

export function PlanValidationTypeV4FromJSONTyped(json: any, ignoreDiscriminator: boolean): PlanValidationTypeV4 {
    return json as PlanValidationTypeV4;
}

export function PlanValidationTypeV4ToJSON(value?: PlanValidationTypeV4 | null): any {
    return value as any;
}

