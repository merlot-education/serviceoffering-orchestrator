package eu.merloteducation.serviceofferingorchestrator.models.gxfscatalog;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class Standard {

    @NotNull
    @JsonProperty("gax-trust-framework:title")
    private StringTypeValue title;

    @NotNull
    @JsonFormat(with = JsonFormat.Feature.ACCEPT_SINGLE_VALUE_AS_ARRAY)
    @JsonProperty("gax-trust-framework:standardReference")
    private List<StringTypeValue> standardReference;

    @JsonProperty("gax-trust-framework:publisher")
    private StringTypeValue publisher;

}
