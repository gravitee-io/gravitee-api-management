/**
 * Gravitee.io Portal Rest API
 * API dedicated to the devportal part of Gravitee
 *
 * The version of the OpenAPI document: 3.14.0-SNAPSHOT
 * Contact: contact@graviteesource.com
 *
 * NOTE: This class is auto generated by OpenAPI Generator (https://openapi-generator.tech).
 * https://openapi-generator.tech
 * Do not edit the class manually.
 */


export interface OAuthClientSettings { 
    client_secret?: string;
    client_id?: string;
    redirect_uris?: Array<string>;
    client_uri?: string;
    logo_uri?: string;
    response_types?: Array<string>;
    grant_types?: Array<string>;
    application_type?: string;
    renew_client_secret_supported?: boolean;
}

