package io.gravitee.repository.mongodb.management.internal.model;

import java.util.List;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

@Getter
@Setter
@EqualsAndHashCode(of = { "id" }, callSuper = false)
@Document(collection = "#{@environment.getProperty('management.mongodb.prefix')}portal_pages")
public class PortalPageMongo {
    @Id
    private String id;
    private String content;
    private List<String> contexts;
}

