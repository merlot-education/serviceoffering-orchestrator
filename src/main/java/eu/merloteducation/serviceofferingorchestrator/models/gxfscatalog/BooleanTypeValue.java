package eu.merloteducation.serviceofferingorchestrator.models.gxfscatalog;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class BooleanTypeValue {
    @NotNull
    @JsonProperty("@type")
    private String type;

    @NotNull
    @JsonProperty("@value")
    private boolean value;

    public BooleanTypeValue(boolean value) {
        this.type = "xsd:boolean";
        this.value = value;
    }
}
