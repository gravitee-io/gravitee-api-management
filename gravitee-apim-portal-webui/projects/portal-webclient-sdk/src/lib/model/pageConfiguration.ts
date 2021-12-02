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


/**
 * Technical information about the page
 */
export interface PageConfiguration { 
    /**
     * Enable \"Try It!\" mode in documentation page.
     */
    try_it?: boolean;
    /**
     * Enable \"Try It!\" mode in documentation page for anonymous users.
     */
    try_it_anonymous?: boolean;
    /**
     * Base URL used to try the API.
     */
    try_it_url?: string;
    /**
     * Show the URL to download the content.
     */
    show_url?: string;
    /**
     * Display the operationId in the operations list.
     */
    display_operation_id?: boolean;
    /**
     * Default expansion setting for the operations and tags.\\ Possibles values are :  - list : Expands only the tags  - full : Expands the tags and operations  - none : Expands nothing. DEFAULT. 
     */
    doc_expansion?: PageConfiguration.DocExpansionEnum;
    /**
     * Add a top bar to filter content.
     */
    enable_filtering?: boolean;
    /**
     * Display vendor extension (X-) fields and values for Operations, Parameters, and Schema.
     */
    show_extensions?: boolean;
    /**
     * Display extensions (pattern, maxLength, minLength, maximum, minimum) fields and values for Parameters.
     */
    show_common_extensions?: boolean;
    /**
     * Number of max tagged operations displayed. \\ Limits the number of tagged operations displayed to at most this many (negative means show all operations).\\ No limit by default. 
     */
    max_displayed_tags?: number;
    /**
     * The type of viewer for OpenAPI specification. Default is \'Swagger\'
     */
    viewer?: PageConfiguration.ViewerEnum;
}
export namespace PageConfiguration {
    export type DocExpansionEnum = 'list' | 'full' | 'none';
    export const DocExpansionEnum = {
        List: 'list' as DocExpansionEnum,
        Full: 'full' as DocExpansionEnum,
        None: 'none' as DocExpansionEnum
    };
    export type ViewerEnum = 'Swagger' | 'Redoc';
    export const ViewerEnum = {
        Swagger: 'Swagger' as ViewerEnum,
        Redoc: 'Redoc' as ViewerEnum
    };
}


