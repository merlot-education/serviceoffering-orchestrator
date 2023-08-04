package eu.merloteducation.serviceofferingorchestrator.models.gxfscatalog;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class Runtime {

    @JsonProperty("merlot:runtimeCount")
    private NumberTypeValue runtimeCount;

    @JsonProperty("merlot:runtimeMeasurement")
    private StringTypeValue runtimeMeasurement;

}
