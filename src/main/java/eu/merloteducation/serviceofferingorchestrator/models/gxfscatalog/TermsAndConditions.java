package eu.merloteducation.serviceofferingorchestrator.models.gxfscatalog;

import com.fasterxml.jackson.annotation.JsonProperty;
import eu.merloteducation.serviceofferingorchestrator.models.gxfscatalog.StringTypeValue;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class TermsAndConditions {

    // mandatory
    @JsonProperty("gax-trust-framework:content")
    private StringTypeValue content;

    // mandatory
    @JsonProperty("gax-trust-framework:hash")
    private StringTypeValue hash;

    @JsonProperty("@type")
    private String type;
}
