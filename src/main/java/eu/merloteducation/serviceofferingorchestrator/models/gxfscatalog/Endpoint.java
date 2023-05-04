package eu.merloteducation.serviceofferingorchestrator.models.gxfscatalog;

import com.fasterxml.jackson.annotation.JsonProperty;
import eu.merloteducation.serviceofferingorchestrator.models.gxfscatalog.StringTypeValue;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class Endpoint {

    @JsonProperty("gax-trust-framework:endPointURL")
    private StringTypeValue endPointURL;

    @JsonProperty("gax-trust-framework:standardConformity")
    private Standard standardConformity;

    @JsonProperty("gax-trust-framework:endpointDescription")
    private StringTypeValue endpointDescription;

    @NotNull
    @JsonProperty("@type")
    private String type;
}
