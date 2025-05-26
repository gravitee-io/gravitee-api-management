package io.gravitee.rest.api.model;

import lombok.Data;

/**
 * @author Benoit BORDIGONI (benoit.bordigoni at graviteesource.com)
 * @author GraviteeSource Team
 */
@Data
public class PluginDocumentationEntity {

    private String content;
    private String language;
}
