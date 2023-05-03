package eu.merloteducation.serviceofferingorchestrator.models.gxfscatalog;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class Standard {

    // mandatory
    @JsonProperty("gax-trust-framework:title")
    private StringTypeValue title;

    // mandatory
    @JsonProperty("gax-trust-framework:standardReference")
    private StringTypeValue standardReference;

    @JsonProperty("gax-trust-framework:publisher")
    private StringTypeValue publisher;

}
