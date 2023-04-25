package eu.merloteducation.serviceofferingorchestrator.models.gxfscatalog;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class StringTypeValue {
    @JsonProperty("@type")
    private String type;
    @JsonProperty("@value")
    private String value;
}
