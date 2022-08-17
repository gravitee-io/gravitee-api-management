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

import { exists, mapValues } from '../runtime';
import {
    ApiLifecycleState,
    ApiLifecycleStateFromJSON,
    ApiLifecycleStateFromJSONTyped,
    ApiLifecycleStateToJSON,
    ApiServicesV4,
    ApiServicesV4FromJSON,
    ApiServicesV4FromJSONTyped,
    ApiServicesV4ToJSON,
    EndpointGroupV4,
    EndpointGroupV4FromJSON,
    EndpointGroupV4FromJSONTyped,
    EndpointGroupV4ToJSON,
    FlowV4,
    FlowV4FromJSON,
    FlowV4FromJSONTyped,
    FlowV4ToJSON,
    ListenerV4,
    ListenerV4FromJSON,
    ListenerV4FromJSONTyped,
    ListenerV4ToJSON,
    PlanEntityV4,
    PlanEntityV4FromJSON,
    PlanEntityV4FromJSONTyped,
    PlanEntityV4ToJSON,
    PrimaryOwnerEntity,
    PrimaryOwnerEntityFromJSON,
    PrimaryOwnerEntityFromJSONTyped,
    PrimaryOwnerEntityToJSON,
    PropertyV4,
    PropertyV4FromJSON,
    PropertyV4FromJSONTyped,
    PropertyV4ToJSON,
    ResourceV4,
    ResourceV4FromJSON,
    ResourceV4FromJSONTyped,
    ResourceV4ToJSON,
    ResponseTemplateV4,
    ResponseTemplateV4FromJSON,
    ResponseTemplateV4FromJSONTyped,
    ResponseTemplateV4ToJSON,
    Visibility,
    VisibilityFromJSON,
    VisibilityFromJSONTyped,
    VisibilityToJSON,
    WorkflowState,
    WorkflowStateFromJSON,
    WorkflowStateFromJSONTyped,
    WorkflowStateToJSON,
} from './';

/**
 * 
 * @export
 * @interface ApiEntityV4
 */
export interface ApiEntityV4 {
    /**
     * Api's version. It's a simple string only used in the portal.
     * @type {string}
     * @memberof ApiEntityV4
     */
    apiVersion?: string;
    /**
     * the API background encoded in base64
     * @type {string}
     * @memberof ApiEntityV4
     */
    background?: string;
    /**
     * the API background url.
     * @type {string}
     * @memberof ApiEntityV4
     */
    backgroundUrl?: string;
    /**
     * the list of categories associated with this API
     * @type {Array<string>}
     * @memberof ApiEntityV4
     */
    categories?: Array<string>;
    /**
     * The date (as a timestamp) when the API was created.
     * @type {Date}
     * @memberof ApiEntityV4
     */
    createdAt?: Date;
    /**
     * API's crossId. Identifies API across environments.
     * @type {string}
     * @memberof ApiEntityV4
     */
    crossId?: string;
    /**
     * API's gravitee definition version
     * @type {string}
     * @memberof ApiEntityV4
     */
    definitionVersion?: ApiEntityV4DefinitionVersionEnum;
    /**
     * The last date (as timestamp) when the API was deployed.
     * @type {Date}
     * @memberof ApiEntityV4
     */
    deployedAt?: Date;
    /**
     * API's description. A short description of your API.
     * @type {string}
     * @memberof ApiEntityV4
     */
    description?: string;
    /**
     * 
     * @type {boolean}
     * @memberof ApiEntityV4
     */
    disableMembershipNotifications?: boolean;
    /**
     * A list of endpoint describing the endpoints to contact.
     * @type {Array<EndpointGroupV4>}
     * @memberof ApiEntityV4
     */
    endpointGroups?: Array<EndpointGroupV4>;
    /**
     * API's flow mode.
     * @type {string}
     * @memberof ApiEntityV4
     */
    flowMode?: ApiEntityV4FlowModeEnum;
    /**
     * A list of flows containing the policies configuration.
     * @type {Array<FlowV4>}
     * @memberof ApiEntityV4
     */
    flows?: Array<FlowV4>;
    /**
     * API's groups. Used to add team in your API.
     * @type {Array<string>}
     * @memberof ApiEntityV4
     */
    groups?: Array<string>;
    /**
     * API's uuid.
     * @type {string}
     * @memberof ApiEntityV4
     */
    id?: string;
    /**
     * the free list of labels associated with this API
     * @type {Array<string>}
     * @memberof ApiEntityV4
     */
    labels?: Array<string>;
    /**
     * 
     * @type {ApiLifecycleState}
     * @memberof ApiEntityV4
     */
    lifecycleState?: ApiLifecycleState;
    /**
     * A list of listeners used to describe our you api could be reached.
     * @type {Array<ListenerV4>}
     * @memberof ApiEntityV4
     */
    listeners?: Array<ListenerV4>;
    /**
     * API's name. Duplicate names can exists.
     * @type {string}
     * @memberof ApiEntityV4
     */
    name?: string;
    /**
     * the API logo encoded in base64
     * @type {string}
     * @memberof ApiEntityV4
     */
    picture?: string;
    /**
     * the API logo URL.
     * @type {string}
     * @memberof ApiEntityV4
     */
    pictureUrl?: string;
    /**
     * A list of plans to apply on the API
     * @type {Array<PlanEntityV4>}
     * @memberof ApiEntityV4
     */
    plans?: Array<PlanEntityV4>;
    /**
     * 
     * @type {PrimaryOwnerEntity}
     * @memberof ApiEntityV4
     */
    primaryOwner?: PrimaryOwnerEntity;
    /**
     * A dictionary (could be dynamic) of properties available in the API context.
     * @type {Array<PropertyV4>}
     * @memberof ApiEntityV4
     */
    properties?: Array<PropertyV4>;
    /**
     * The list of API resources used by policies like cache resources or oauth2
     * @type {Array<ResourceV4>}
     * @memberof ApiEntityV4
     */
    resources?: Array<ResourceV4>;
    /**
     * A map that allows you to configure the output of a request based on the event throws by the gateway. Example : Quota exceeded, api-key is missing, ...
     * @type {{ [key: string]: { [key: string]: ResponseTemplateV4; }; }}
     * @memberof ApiEntityV4
     */
    responseTemplates?: { [key: string]: { [key: string]: ResponseTemplateV4; }; };
    /**
     * 
     * @type {ApiServicesV4}
     * @memberof ApiEntityV4
     */
    services?: ApiServicesV4;
    /**
     * The status of the API regarding the gateway.
     * @type {string}
     * @memberof ApiEntityV4
     */
    state?: ApiEntityV4StateEnum;
    /**
     * The list of sharding tags associated with this API.
     * @type {Array<string>}
     * @memberof ApiEntityV4
     */
    tags?: Array<string>;
    /**
     * API's type
     * @type {string}
     * @memberof ApiEntityV4
     */
    type?: ApiEntityV4TypeEnum;
    /**
     * The last date (as a timestamp) when the API was updated.
     * @type {Date}
     * @memberof ApiEntityV4
     */
    updatedAt?: Date;
    /**
     * 
     * @type {Visibility}
     * @memberof ApiEntityV4
     */
    visibility?: Visibility;
    /**
     * 
     * @type {WorkflowState}
     * @memberof ApiEntityV4
     */
    workflowState?: WorkflowState;
}

export function ApiEntityV4FromJSON(json: any): ApiEntityV4 {
    return ApiEntityV4FromJSONTyped(json, false);
}

export function ApiEntityV4FromJSONTyped(json: any, ignoreDiscriminator: boolean): ApiEntityV4 {
    if ((json === undefined) || (json === null)) {
        return json;
    }
    return {
        
        'apiVersion': !exists(json, 'apiVersion') ? undefined : json['apiVersion'],
        'background': !exists(json, 'background') ? undefined : json['background'],
        'backgroundUrl': !exists(json, 'backgroundUrl') ? undefined : json['backgroundUrl'],
        'categories': !exists(json, 'categories') ? undefined : json['categories'],
        'createdAt': !exists(json, 'createdAt') ? undefined : (new Date(json['createdAt'])),
        'crossId': !exists(json, 'crossId') ? undefined : json['crossId'],
        'definitionVersion': !exists(json, 'definitionVersion') ? undefined : json['definitionVersion'],
        'deployedAt': !exists(json, 'deployedAt') ? undefined : (new Date(json['deployedAt'])),
        'description': !exists(json, 'description') ? undefined : json['description'],
        'disableMembershipNotifications': !exists(json, 'disableMembershipNotifications') ? undefined : json['disableMembershipNotifications'],
        'endpointGroups': !exists(json, 'endpointGroups') ? undefined : ((json['endpointGroups'] as Array<any>).map(EndpointGroupV4FromJSON)),
        'flowMode': !exists(json, 'flowMode') ? undefined : json['flowMode'],
        'flows': !exists(json, 'flows') ? undefined : ((json['flows'] as Array<any>).map(FlowV4FromJSON)),
        'groups': !exists(json, 'groups') ? undefined : json['groups'],
        'id': !exists(json, 'id') ? undefined : json['id'],
        'labels': !exists(json, 'labels') ? undefined : json['labels'],
        'lifecycleState': !exists(json, 'lifecycleState') ? undefined : ApiLifecycleStateFromJSON(json['lifecycleState']),
        'listeners': !exists(json, 'listeners') ? undefined : ((json['listeners'] as Array<any>).map(ListenerV4FromJSON)),
        'name': !exists(json, 'name') ? undefined : json['name'],
        'picture': !exists(json, 'picture') ? undefined : json['picture'],
        'pictureUrl': !exists(json, 'pictureUrl') ? undefined : json['pictureUrl'],
        'plans': !exists(json, 'plans') ? undefined : ((json['plans'] as Array<any>).map(PlanEntityV4FromJSON)),
        'primaryOwner': !exists(json, 'primaryOwner') ? undefined : PrimaryOwnerEntityFromJSON(json['primaryOwner']),
        'properties': !exists(json, 'properties') ? undefined : ((json['properties'] as Array<any>).map(PropertyV4FromJSON)),
        'resources': !exists(json, 'resources') ? undefined : ((json['resources'] as Array<any>).map(ResourceV4FromJSON)),
        'responseTemplates': !exists(json, 'responseTemplates') ? undefined : json['responseTemplates'],
        'services': !exists(json, 'services') ? undefined : ApiServicesV4FromJSON(json['services']),
        'state': !exists(json, 'state') ? undefined : json['state'],
        'tags': !exists(json, 'tags') ? undefined : json['tags'],
        'type': !exists(json, 'type') ? undefined : json['type'],
        'updatedAt': !exists(json, 'updatedAt') ? undefined : (new Date(json['updatedAt'])),
        'visibility': !exists(json, 'visibility') ? undefined : VisibilityFromJSON(json['visibility']),
        'workflowState': !exists(json, 'workflowState') ? undefined : WorkflowStateFromJSON(json['workflowState']),
    };
}

export function ApiEntityV4ToJSON(value?: ApiEntityV4 | null): any {
    if (value === undefined) {
        return undefined;
    }
    if (value === null) {
        return null;
    }
    return {
        
        'apiVersion': value.apiVersion,
        'background': value.background,
        'backgroundUrl': value.backgroundUrl,
        'categories': value.categories,
        'createdAt': value.createdAt === undefined ? undefined : (value.createdAt.toISOString()),
        'crossId': value.crossId,
        'definitionVersion': value.definitionVersion,
        'deployedAt': value.deployedAt === undefined ? undefined : (value.deployedAt.toISOString()),
        'description': value.description,
        'disableMembershipNotifications': value.disableMembershipNotifications,
        'endpointGroups': value.endpointGroups === undefined ? undefined : ((value.endpointGroups as Array<any>).map(EndpointGroupV4ToJSON)),
        'flowMode': value.flowMode,
        'flows': value.flows === undefined ? undefined : ((value.flows as Array<any>).map(FlowV4ToJSON)),
        'groups': value.groups,
        'id': value.id,
        'labels': value.labels,
        'lifecycleState': ApiLifecycleStateToJSON(value.lifecycleState),
        'listeners': value.listeners === undefined ? undefined : ((value.listeners as Array<any>).map(ListenerV4ToJSON)),
        'name': value.name,
        'picture': value.picture,
        'pictureUrl': value.pictureUrl,
        'plans': value.plans === undefined ? undefined : ((value.plans as Array<any>).map(PlanEntityV4ToJSON)),
        'primaryOwner': PrimaryOwnerEntityToJSON(value.primaryOwner),
        'properties': value.properties === undefined ? undefined : ((value.properties as Array<any>).map(PropertyV4ToJSON)),
        'resources': value.resources === undefined ? undefined : ((value.resources as Array<any>).map(ResourceV4ToJSON)),
        'responseTemplates': value.responseTemplates,
        'services': ApiServicesV4ToJSON(value.services),
        'state': value.state,
        'tags': value.tags,
        'type': value.type,
        'updatedAt': value.updatedAt === undefined ? undefined : (value.updatedAt.toISOString()),
        'visibility': VisibilityToJSON(value.visibility),
        'workflowState': WorkflowStateToJSON(value.workflowState),
    };
}

/**
* @export
* @enum {string}
*/
export enum ApiEntityV4DefinitionVersionEnum {
    V1 = 'V1',
    V2 = 'V2',
    V4 = 'V4'
}
/**
* @export
* @enum {string}
*/
export enum ApiEntityV4FlowModeEnum {
    DEFAULT = 'DEFAULT',
    BESTMATCH = 'BEST_MATCH'
}
/**
* @export
* @enum {string}
*/
export enum ApiEntityV4StateEnum {
    INITIALIZED = 'INITIALIZED',
    STOPPED = 'STOPPED',
    STOPPING = 'STOPPING',
    STARTED = 'STARTED',
    CLOSED = 'CLOSED'
}
/**
* @export
* @enum {string}
*/
export enum ApiEntityV4TypeEnum {
    SYNC = 'SYNC',
    ASYNC = 'ASYNC'
}


