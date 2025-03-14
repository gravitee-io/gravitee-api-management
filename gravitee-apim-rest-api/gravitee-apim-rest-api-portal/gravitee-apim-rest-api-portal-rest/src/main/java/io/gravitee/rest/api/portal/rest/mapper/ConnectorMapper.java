package io.gravitee.rest.api.portal.rest.mapper;

import io.gravitee.rest.api.model.v4.connector.ConnectorExpandPluginEntity;
import io.gravitee.rest.api.portal.rest.model.Connector;
import org.mapstruct.Mapper;
import org.mapstruct.factory.Mappers;
import java.util.Collection;
import java.util.List;

@Mapper
public interface ConnectorMapper {
    ConnectorMapper INSTANCE = Mappers.getMapper(ConnectorMapper.class);

    Connector convert(ConnectorExpandPluginEntity connectorExpandPluginEntity);
    List<Connector> convert(Collection<ConnectorExpandPluginEntity> connectorExpandPluginEntities);
}
