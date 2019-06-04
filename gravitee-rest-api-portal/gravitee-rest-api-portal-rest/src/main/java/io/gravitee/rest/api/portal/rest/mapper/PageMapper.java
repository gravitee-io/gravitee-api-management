/**
 * Copyright (C) 2015 The Gravitee team (http://gravitee.io)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.gravitee.rest.api.portal.rest.mapper;

import java.time.ZoneOffset;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

import org.springframework.stereotype.Component;

import io.gravitee.rest.api.model.PageEntity;
import io.gravitee.rest.api.portal.rest.model.Metadata;
import io.gravitee.rest.api.portal.rest.model.Page;
import io.gravitee.rest.api.portal.rest.model.Page.TypeEnum;
import io.gravitee.rest.api.portal.rest.model.PageConfiguration;

/**
 * @author Florent CHAMFROY (florent.chamfroy at graviteesource.com)
 * @author GraviteeSource Team
 */
@Component
public class PageMapper {
    
    public Page convert(PageEntity page) {
        final Page pageItem = new Page();

        if(page.getConfiguration() != null) {
            List<PageConfiguration> pageConfigurationList = page.getConfiguration().entrySet()
                    .stream()
                    .map(e-> new PageConfiguration()
                            .key(e.getKey())
                            .value(e.getValue())
                            )
                    .collect(Collectors.toList());
            pageItem.setConfiguraton(pageConfigurationList);
        }
        pageItem.setContent(page.getContent());
        pageItem.setId(page.getId());
        
        if(page.getMetadata() != null) {
            AtomicInteger counter = new AtomicInteger(0);
            List<Metadata> metadataList = page.getMetadata().entrySet()
                    .stream()
                    .map(e-> new Metadata()
                            .name(e.getKey())
                            .value(e.getValue())
                            .order(Integer.toString(counter.getAndIncrement()))
                        )
                    .collect(Collectors.toList());
            pageItem.setMetadata(metadataList);
        }
        pageItem.setName(page.getName());
        pageItem.setOrder(page.getOrder());
        pageItem.setParent(page.getParentId());
        if(page.getType() != null) {
            pageItem.setType(TypeEnum.fromValue(page.getType()));
        }
        if(page.getLastModificationDate() != null) {
            pageItem.setUpdatedAt(page.getLastModificationDate().toInstant().atOffset( ZoneOffset.UTC ));
        }
        
        return pageItem;
    }

}
