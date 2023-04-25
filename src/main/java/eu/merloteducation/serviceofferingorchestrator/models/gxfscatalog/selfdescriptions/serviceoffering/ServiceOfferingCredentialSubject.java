package eu.merloteducation.serviceofferingorchestrator.models.gxfscatalog.selfdescriptions.serviceoffering;

import com.fasterxml.jackson.annotation.JsonProperty;
import eu.merloteducation.serviceofferingorchestrator.models.gxfscatalog.*;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ServiceOfferingCredentialSubject {
    @JsonProperty("@id")
    private String id;

    @JsonProperty("@type")
    private String type;

    @JsonProperty("gax-trust-framework:policy")
    private StringTypeValue policy;

    @JsonProperty("gax-trust-framework:name")
    private StringTypeValue name;

    @JsonProperty("gax-trust-framework:providedBy")
    private NodeKindIRITypeId providedBy;

    @JsonProperty("gax-core:offeredBy")
    private NodeKindIRITypeId offeredBy;

    @JsonProperty("gax-trust-framework:endpoint")
    private Endpoint endpoint;

    @JsonProperty("gax-trust-framework:dataAccountExport")
    private DataAccountExport dataAccountExport;

    @JsonProperty("gax-trust-framework:termsAndConditions")
    private TermsAndConditions termsAndConditions;

}
