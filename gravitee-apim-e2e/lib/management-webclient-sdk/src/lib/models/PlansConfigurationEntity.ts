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

import { exists, mapValues } from '../runtime';
import type { PlanSecurityEntity } from './PlanSecurityEntity';
import {
    PlanSecurityEntityFromJSON,
    PlanSecurityEntityFromJSONTyped,
    PlanSecurityEntityToJSON,
} from './PlanSecurityEntity';

/**
 * 
 * @export
 * @interface PlansConfigurationEntity
 */
export interface PlansConfigurationEntity {
    /**
     * 
     * @type {Array<PlanSecurityEntity>}
     * @memberof PlansConfigurationEntity
     */
    security?: Array<PlanSecurityEntity>;
}

/**
 * Check if a given object implements the PlansConfigurationEntity interface.
 */
export function instanceOfPlansConfigurationEntity(value: object): boolean {
    let isInstance = true;

    return isInstance;
}

export function PlansConfigurationEntityFromJSON(json: any): PlansConfigurationEntity {
    return PlansConfigurationEntityFromJSONTyped(json, false);
}

export function PlansConfigurationEntityFromJSONTyped(json: any, ignoreDiscriminator: boolean): PlansConfigurationEntity {
    if ((json === undefined) || (json === null)) {
        return json;
    }
    return {
        
        'security': !exists(json, 'security') ? undefined : ((json['security'] as Array<any>).map(PlanSecurityEntityFromJSON)),
    };
}

export function PlansConfigurationEntityToJSON(value?: PlansConfigurationEntity | null): any {
    if (value === undefined) {
        return undefined;
    }
    if (value === null) {
        return null;
    }
    return {
        
        'security': value.security === undefined ? undefined : ((value.security as Array<any>).map(PlanSecurityEntityToJSON)),
    };
}

