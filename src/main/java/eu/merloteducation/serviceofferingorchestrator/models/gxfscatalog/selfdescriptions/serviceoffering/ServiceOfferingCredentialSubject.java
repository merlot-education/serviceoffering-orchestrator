package eu.merloteducation.serviceofferingorchestrator.models.gxfscatalog.selfdescriptions.serviceoffering;

import com.fasterxml.jackson.annotation.JsonProperty;
import eu.merloteducation.serviceofferingorchestrator.models.gxfscatalog.*;
import eu.merloteducation.serviceofferingorchestrator.models.gxfscatalog.Runtime;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

import java.util.List;
import java.util.Map;

@Getter
@Setter
public class ServiceOfferingCredentialSubject {

    // general catalog fields

    @NotNull
    @JsonProperty("@id")
    private String id;

    @NotNull
    @JsonProperty("@type")
    private String type;

    @NotNull
    @JsonProperty("@context")
    private Map<String, String> context;

    // inherited from gax-core:ServiceOffering

    @NotNull
    @JsonProperty("gax-core:offeredBy")
    private NodeKindIRITypeId offeredBy;

    @JsonProperty("gax-core:aggregationOf")
    private NodeKindIRITypeId coreAggregationOf;

    @JsonProperty("gax-core:dependsOn")
    private NodeKindIRITypeId coreDependsOn;

    // inherited from gax-trust-framework:ServiceOffering

    @NotNull
    @JsonProperty("gax-trust-framework:name")
    private StringTypeValue name;

    @NotNull
    @JsonProperty("gax-trust-framework:termsAndConditions")
    private TermsAndConditions termsAndConditions;

    @NotNull
    @JsonProperty("gax-trust-framework:policy")
    private StringTypeValue policy;

    @JsonProperty("gax-trust-framework:dataProtectionRegime")
    private StringTypeValue dataProtectionRegime;

    @NotNull
    @JsonProperty("gax-trust-framework:dataAccountExport")
    private DataAccountExport dataAccountExport;

    @JsonProperty("dct:description")
    private StringTypeValue description;

    @JsonProperty("dcat:keyword")
    private StringTypeValue keyword;

    @JsonProperty("gax-trust-framework:provisionType")
    private StringTypeValue provisionType;

    @JsonProperty("gax-trust-framework:endpoint")
    private Endpoint endpoint;

    @NotNull
    @JsonProperty("gax-trust-framework:providedBy")
    private NodeKindIRITypeId providedBy;

    @JsonProperty("gax-trust-framework:aggregationOf")
    private NodeKindIRITypeId trustAggregationOf;

    @JsonProperty("gax-trust-framework:dependsOn")
    private StringTypeValue trustDependsOn;

    @JsonProperty("gax-trust-framework:ServiceOfferingLocations")
    private StringTypeValue serviceOfferingLocations;

    // inherited from merlot:MerlotServiceOffering

    @NotNull
    @JsonProperty("merlot:creationDate")
    private StringTypeValue creationDate;

    @NotNull
    @JsonProperty("merlot:dataAccessType")
    private StringTypeValue dataAccessType;

    @JsonProperty("merlot:attachments")
    private StringTypeValue attachments;

    @JsonProperty("merlot:exampleCosts")
    private StringTypeValue exampleCosts;

    @NotNull
    @JsonProperty("merlot:runtimeOption")
    private Runtime runtimes;

    @NotNull
    @JsonProperty("merlot:merlotTermsAndConditionsAccepted")
    private boolean merlotTermsAndConditionsAccepted;
}
