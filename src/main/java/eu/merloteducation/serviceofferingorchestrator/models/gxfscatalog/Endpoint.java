package eu.merloteducation.serviceofferingorchestrator.models.gxfscatalog;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class Endpoint {

    @JsonFormat(with = JsonFormat.Feature.ACCEPT_SINGLE_VALUE_AS_ARRAY)
    @JsonProperty("gax-trust-framework:endPointURL")
    private List<StringTypeValue> endPointURL;

    @JsonFormat(with = JsonFormat.Feature.ACCEPT_SINGLE_VALUE_AS_ARRAY)
    @JsonProperty("gax-trust-framework:standardConformity")
    private List<Standard> standardConformity;

    @JsonFormat(with = JsonFormat.Feature.ACCEPT_SINGLE_VALUE_AS_ARRAY)
    @JsonProperty("gax-trust-framework:endpointDescription")
    private List<StringTypeValue> endpointDescription;

    @NotNull
    @JsonProperty("@type")
    private String type;
}
