package io.gravitee.rest.api.management.v2.rest.mapper;

import io.gravitee.plugin.core.api.PluginDocumentation;
import io.gravitee.rest.api.model.PluginDocumentationEntity;
import org.mapstruct.Mapper;
import org.mapstruct.NullValueMappingStrategy;
import org.mapstruct.factory.Mappers;

/**
 * @author Benoit BORDIGONI (benoit.bordigoni at graviteesource.com)
 * @author GraviteeSource Team
 */
@Mapper(nullValueIterableMappingStrategy = NullValueMappingStrategy.RETURN_DEFAULT)
public interface PluginDocumentationMapper {
    PluginDocumentationMapper INSTANCE = Mappers.getMapper(PluginDocumentationMapper.class);
    PluginDocumentationEntity map(PluginDocumentation documentation);
}
