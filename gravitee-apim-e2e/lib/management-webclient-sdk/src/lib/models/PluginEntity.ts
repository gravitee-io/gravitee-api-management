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
 * @interface PluginEntity
 */
export interface PluginEntity {
    /**
     * 
     * @type {Array<string>}
     * @memberof PluginEntity
     */
    dependencies?: Array<string>;
    /**
     * 
     * @type {string}
     * @memberof PluginEntity
     */
    description?: string;
    /**
     * 
     * @type {string}
     * @memberof PluginEntity
     */
    id?: string;
    /**
     * 
     * @type {string}
     * @memberof PluginEntity
     */
    name?: string;
    /**
     * 
     * @type {string}
     * @memberof PluginEntity
     */
    path?: string;
    /**
     * 
     * @type {string}
     * @memberof PluginEntity
     */
    plugin?: string;
    /**
     * 
     * @type {string}
     * @memberof PluginEntity
     */
    type?: string;
    /**
     * 
     * @type {string}
     * @memberof PluginEntity
     */
    version?: string;
}

/**
 * Check if a given object implements the PluginEntity interface.
 */
export function instanceOfPluginEntity(value: object): boolean {
    let isInstance = true;

    return isInstance;
}

export function PluginEntityFromJSON(json: any): PluginEntity {
    return PluginEntityFromJSONTyped(json, false);
}

export function PluginEntityFromJSONTyped(json: any, ignoreDiscriminator: boolean): PluginEntity {
    if ((json === undefined) || (json === null)) {
        return json;
    }
    return {
        
        'dependencies': !exists(json, 'dependencies') ? undefined : json['dependencies'],
        'description': !exists(json, 'description') ? undefined : json['description'],
        'id': !exists(json, 'id') ? undefined : json['id'],
        'name': !exists(json, 'name') ? undefined : json['name'],
        'path': !exists(json, 'path') ? undefined : json['path'],
        'plugin': !exists(json, 'plugin') ? undefined : json['plugin'],
        'type': !exists(json, 'type') ? undefined : json['type'],
        'version': !exists(json, 'version') ? undefined : json['version'],
    };
}

export function PluginEntityToJSON(value?: PluginEntity | null): any {
    if (value === undefined) {
        return undefined;
    }
    if (value === null) {
        return null;
    }
    return {
        
        'dependencies': value.dependencies,
        'description': value.description,
        'id': value.id,
        'name': value.name,
        'path': value.path,
        'plugin': value.plugin,
        'type': value.type,
        'version': value.version,
    };
}

