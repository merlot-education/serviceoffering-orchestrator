package eu.merloteducation.serviceofferingorchestrator.models.gxfscatalog;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
public class StringTypeValue {
    @NotNull
    @JsonProperty("@type")
    private String type;

    @NotNull
    @JsonProperty("@value")
    private String value;
}
