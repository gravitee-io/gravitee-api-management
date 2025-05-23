package io.gravitee.rest.api.service.impl.upgrade.upgrader;

import static io.gravitee.rest.api.service.impl.upgrade.upgrader.UpgraderOrder.DICTIONARY_ID_UPGRADER;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.gravitee.common.data.domain.Page;
import io.gravitee.node.api.upgrader.Upgrader;
import io.gravitee.repository.exceptions.TechnicalException;
import io.gravitee.repository.management.api.DictionaryRepository;
import io.gravitee.repository.management.api.EventLatestRepository;
import io.gravitee.repository.management.api.EventRepository;
import io.gravitee.repository.management.api.search.EventCriteria;
import io.gravitee.repository.management.api.search.builder.PageableBuilder;
import io.gravitee.repository.management.model.Dictionary;
import io.gravitee.repository.management.model.DictionaryType;
import io.gravitee.repository.management.model.Event;
import io.gravitee.repository.management.model.EventType;
import io.gravitee.rest.api.service.EventService;
import io.gravitee.rest.api.service.common.UuidString;
import java.util.Set;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class DictionaryIdUpgrader implements Upgrader {

    private final DictionaryRepository dictionaryRepository;
    private final EventRepository eventRepository;
    private final EventLatestRepository eventLatestRepository;
    private final ObjectMapper objectMapper;

    @Autowired
    public DictionaryIdUpgrader(
        @Lazy DictionaryRepository dictionaryRepository,
        @Lazy EventRepository eventRepository,
        @Lazy EventLatestRepository eventLatestRepository,
        @Lazy ObjectMapper objectMapper
    ) {
        this.dictionaryRepository = dictionaryRepository;
        this.eventRepository = eventRepository;
        this.eventLatestRepository = eventLatestRepository;
        this.objectMapper = objectMapper;
    }

    @Override
    public String version() {
        return "v1";
    }

    @Override
    public boolean upgrade() {
        try {
            Set<Dictionary> dictionaries = dictionaryRepository.findAll();
            for (Dictionary dictionary : dictionaries) {
                if (dictionary.getKey() == null) {
                    String oldId = dictionary.getId();
                    dictionary.setKey(oldId);
                    dictionary.setId(UuidString.generateRandom());

                    dictionaryRepository.delete(oldId);
                    dictionaryRepository.create(dictionary);
                }
            }
        } catch (TechnicalException e) {
            log.error("Error applying upgrader", e);
            return false;
        }

        return true;
    }

    @Override
    public int getOrder() {
        return DICTIONARY_ID_UPGRADER;
    }
}
