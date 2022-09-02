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
 * @interface StepV4
 */
export interface StepV4 {
    /**
     * 
     * @type {string}
     * @memberof StepV4
     */
    condition?: string;
    /**
     * 
     * @type {any}
     * @memberof StepV4
     */
    configuration?: any;
    /**
     * 
     * @type {string}
     * @memberof StepV4
     */
    description: string;
    /**
     * 
     * @type {boolean}
     * @memberof StepV4
     */
    enabled?: boolean;
    /**
     * 
     * @type {string}
     * @memberof StepV4
     */
    name: string;
    /**
     * 
     * @type {string}
     * @memberof StepV4
     */
    policy: string;
}

/**
 * Check if a given object implements the StepV4 interface.
 */
export function instanceOfStepV4(value: object): boolean {
    let isInstance = true;
    isInstance = isInstance && "description" in value;
    isInstance = isInstance && "name" in value;
    isInstance = isInstance && "policy" in value;

    return isInstance;
}

export function StepV4FromJSON(json: any): StepV4 {
    return StepV4FromJSONTyped(json, false);
}

export function StepV4FromJSONTyped(json: any, ignoreDiscriminator: boolean): StepV4 {
    if ((json === undefined) || (json === null)) {
        return json;
    }
    return {
        
        'condition': !exists(json, 'condition') ? undefined : json['condition'],
        'configuration': !exists(json, 'configuration') ? undefined : json['configuration'],
        'description': json['description'],
        'enabled': !exists(json, 'enabled') ? undefined : json['enabled'],
        'name': json['name'],
        'policy': json['policy'],
    };
}

export function StepV4ToJSON(value?: StepV4 | null): any {
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

