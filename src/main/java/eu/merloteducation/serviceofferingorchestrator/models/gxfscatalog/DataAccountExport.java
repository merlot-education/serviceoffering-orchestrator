package eu.merloteducation.serviceofferingorchestrator.models.gxfscatalog;

import com.fasterxml.jackson.annotation.JsonProperty;
import eu.merloteducation.serviceofferingorchestrator.models.gxfscatalog.StringTypeValue;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class DataAccountExport {
    @JsonProperty("gax-trust-framework:formatType")
    private StringTypeValue formatType;

    @JsonProperty("gax-trust-framework:accessType")
    private StringTypeValue accessType;

    @JsonProperty("gax-trust-framework:requestType")
    private StringTypeValue requestType;

    @JsonProperty("@type")
    private String type;
}
