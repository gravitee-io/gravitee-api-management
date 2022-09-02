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
import type { ConditionSelectorV4AllOf } from './ConditionSelectorV4AllOf';
import {
    ConditionSelectorV4AllOfFromJSON,
    ConditionSelectorV4AllOfFromJSONTyped,
    ConditionSelectorV4AllOfToJSON,
} from './ConditionSelectorV4AllOf';
import type { Selector } from './Selector';
import {
    SelectorFromJSON,
    SelectorFromJSONTyped,
    SelectorToJSON,
} from './Selector';

/**
 * 
 * @export
 * @interface ConditionSelectorV4
 */
export interface ConditionSelectorV4 extends Selector {
    /**
     * 
     * @type {string}
     * @memberof ConditionSelectorV4
     */
    condition: string;
}



/**
 * Check if a given object implements the ConditionSelectorV4 interface.
 */
export function instanceOfConditionSelectorV4(value: object): boolean {
    let isInstance = true;
    isInstance = isInstance && "condition" in value;

    return isInstance;
}

export function ConditionSelectorV4FromJSON(json: any): ConditionSelectorV4 {
    return ConditionSelectorV4FromJSONTyped(json, false);
}

export function ConditionSelectorV4FromJSONTyped(json: any, ignoreDiscriminator: boolean): ConditionSelectorV4 {
    if ((json === undefined) || (json === null)) {
        return json;
    }
    return {
        ...SelectorFromJSONTyped(json, ignoreDiscriminator),
        'condition': json['condition'],
    };
}

export function ConditionSelectorV4ToJSON(value?: ConditionSelectorV4 | null): any {
    if (value === undefined) {
        return undefined;
    }
    if (value === null) {
        return null;
    }
    return {
        ...SelectorToJSON(value),
        'condition': value.condition,
    };
}

