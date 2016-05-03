package io.gravitee.management.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import io.gravitee.definition.jackson.datatype.services.core.deser.ServiceDeserializer;
import io.gravitee.definition.jackson.datatype.services.core.ser.ServiceSerializer;
import io.gravitee.definition.model.Service;

import java.util.List;

/**
 * @author David BRASSELY (david at gravitee.io)
 * @author GraviteeSource Team
 */
public class ServicesEntity {

    @JsonProperty(value = "services")
    @JsonDeserialize(contentUsing = ServiceDeserializer.class)
    @JsonSerialize(contentUsing = ServiceSerializer.class)
    private List<Service> services;

    public List<Service> getServices() {
        return services;
    }

    public void setServices(List<Service> services) {
        this.services = services;
    }
}
