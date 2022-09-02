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
/**
 * 
 * @export
 * @interface Step
 */
export interface Step {
    /**
     * 
     * @type {string}
     * @memberof Step
     */
    condition?: string;
    /**
     * 
     * @type {any}
     * @memberof Step
     */
    configuration?: any;
    /**
     * 
     * @type {string}
     * @memberof Step
     */
    description?: string;
    /**
     * 
     * @type {boolean}
     * @memberof Step
     */
    enabled?: boolean;
    /**
     * 
     * @type {string}
     * @memberof Step
     */
    name?: string;
    /**
     * 
     * @type {string}
     * @memberof Step
     */
    policy?: string;
}

/**
 * Check if a given object implements the Step interface.
 */
export function instanceOfStep(value: object): boolean {
    let isInstance = true;

    return isInstance;
}

export function StepFromJSON(json: any): Step {
    return StepFromJSONTyped(json, false);
}

export function StepFromJSONTyped(json: any, ignoreDiscriminator: boolean): Step {
    if ((json === undefined) || (json === null)) {
        return json;
    }
    return {
        
        'condition': !exists(json, 'condition') ? undefined : json['condition'],
        'configuration': !exists(json, 'configuration') ? undefined : json['configuration'],
        'description': !exists(json, 'description') ? undefined : json['description'],
        'enabled': !exists(json, 'enabled') ? undefined : json['enabled'],
        'name': !exists(json, 'name') ? undefined : json['name'],
        'policy': !exists(json, 'policy') ? undefined : json['policy'],
    };
}

export function StepToJSON(value?: Step | null): any {
    if (value === undefined) {
        return undefined;
    }
    if (value === null) {
        return null;
    }
    return {
        
        'condition': value.condition,
        'configuration': value.configuration,
        'description': value.description,
        'enabled': value.enabled,
        'name': value.name,
        'policy': value.policy,
    };
}

