package io.gravitee.apim.infra.adapter;

import io.gravitee.apim.core.portal_page.model.PortalPage;
import io.gravitee.apim.core.portal_page.model.PortalPageFactory;
import io.gravitee.apim.core.portal_page.model.PortalViewContext;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

public class PortalPageAdapter {
    public static PortalPage toDomain(io.gravitee.repository.management.model.PortalPage entity) {
        if (entity == null) return null;
        return PortalPageFactory.createGraviteeMarkdownPage(entity.getId(), entity.getContent());
    }

    public static io.gravitee.repository.management.model.PortalPage toEntity(PortalPage domain, List<PortalViewContext> contexts) {
        if (domain == null) return null;
        io.gravitee.repository.management.model.PortalPage entity = new io.gravitee.repository.management.model.PortalPage();
        entity.setId(domain.id().id().toString());
        entity.setContent(domain.pageContent().content());
        if (contexts != null) {
            entity.setContexts(contexts.stream().map(Enum::name).collect(Collectors.toList()));
        } else {
            entity.setContexts(Collections.emptyList());
        }
        return entity;
    }

    public static List<PortalPage> toDomainList(List<io.gravitee.repository.management.model.PortalPage> entities) {
        if (entities == null) return Collections.emptyList();
        return entities.stream().map(PortalPageAdapter::toDomain).collect(Collectors.toList());
    }

    public static List<io.gravitee.repository.management.model.PortalPage> toEntityList(List<PortalPage> domains, List<List<PortalViewContext>> contextsList) {
        if (domains == null) return Collections.emptyList();
        return domains.stream().map(domain -> {
            int idx = domains.indexOf(domain);
            List<PortalViewContext> ctx = (contextsList != null && idx < contextsList.size()) ? contextsList.get(idx) : null;
            return toEntity(domain, ctx);
        }).collect(Collectors.toList());
    }

    public static List<PortalViewContext> toContextList(List<String> contextStrings) {
        if (contextStrings == null) return Collections.emptyList();
        return contextStrings.stream()
            .map(s -> {
                try {
                    return PortalViewContext.valueOf(s);
                } catch (Exception e) {
                    return null;
                }
            })
            .filter(Objects::nonNull)
            .collect(Collectors.toList());
    }

    public static List<String> toContextStringList(List<PortalViewContext> contexts) {
        if (contexts == null) return Collections.emptyList();
        return contexts.stream().map(Enum::name).collect(Collectors.toList());
    }
}
