package eu.merloteducation.serviceofferingorchestrator.models.gxfscatalog;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class Standard {

    @NotNull
    @JsonProperty("gax-trust-framework:title")
    private StringTypeValue title;

    @NotNull
    @JsonProperty("gax-trust-framework:standardReference")
    private StringTypeValue standardReference;

    @JsonProperty("gax-trust-framework:publisher")
    private StringTypeValue publisher;

}
