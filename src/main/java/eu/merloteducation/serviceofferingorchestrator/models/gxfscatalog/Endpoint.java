package eu.merloteducation.serviceofferingorchestrator.models.gxfscatalog;

import com.fasterxml.jackson.annotation.JsonProperty;
import eu.merloteducation.serviceofferingorchestrator.models.gxfscatalog.StringTypeValue;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class Endpoint {
    @JsonProperty("gax-trust-framework:endPointURL")
    private StringTypeValue endPointURL;
    @JsonProperty("@type")
    private String type;
}
