package eu.merloteducation.serviceofferingorchestrator.models.gxfscatalog;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class DataAccountExport {

    @NotNull
    @JsonProperty("gax-trust-framework:formatType")
    private StringTypeValue formatType;

    @NotNull
    @JsonProperty("gax-trust-framework:accessType")
    private StringTypeValue accessType;

    @NotNull
    @JsonProperty("gax-trust-framework:requestType")
    private StringTypeValue requestType;

    @NotNull
    @JsonProperty("@type")
    private String type;
}
