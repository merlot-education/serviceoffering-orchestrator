package eu.merloteducation.serviceofferingorchestrator.models.gxfscatalog.selfdescriptions.serviceoffering;

import com.fasterxml.jackson.annotation.JsonProperty;
import eu.merloteducation.serviceofferingorchestrator.models.gxfscatalog.*;
import lombok.Getter;
import lombok.Setter;

import java.util.Map;

@Getter
@Setter
public class ServiceOfferingCredentialSubject {

    // general catalog fields

    // mandatory
    @JsonProperty("@id")
    private String id;

    // mandatory
    @JsonProperty("@type")
    private String type;

    // mandatory
    @JsonProperty("@context")
    private Map<String, String> context;

    // inherited from gax-core:ServiceOffering

    // mandatory
    @JsonProperty("gax-core:offeredBy")
    private NodeKindIRITypeId offeredBy;

    @JsonProperty("gax-core:aggregationOf")
    private NodeKindIRITypeId coreAggregationOf;

    @JsonProperty("gax-core:dependsOn")
    private NodeKindIRITypeId coreDependsOn;

    // inherited from gax-trust-framework:ServiceOffering

    // mandatory
    @JsonProperty("gax-trust-framework:name")
    private StringTypeValue name;

    // mandatory
    @JsonProperty("gax-trust-framework:termsAndConditions")
    private TermsAndConditions termsAndConditions;

    // mandatory
    @JsonProperty("gax-trust-framework:policy")
    private StringTypeValue policy;

    @JsonProperty("gax-trust-framework:dataProtectionRegime")
    private StringTypeValue dataProtectionRegime;

    // mandatory
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

    // mandatory
    @JsonProperty("gax-trust-framework:providedBy")
    private NodeKindIRITypeId providedBy;

    @JsonProperty("gax-trust-framework:aggregationOf")
    private NodeKindIRITypeId trustAggregationOf;

    @JsonProperty("gax-trust-framework:dependsOn")
    private StringTypeValue trustDependsOn;

    @JsonProperty("gax-trust-framework:ServiceOfferingLocations")
    private StringTypeValue serviceOfferingLocations;

    // inherited from merlot:MerlotServiceOffering

    // mandatory
    @JsonProperty("merlot:serviceId")
    private StringTypeValue serviceId;

    // mandatory
    @JsonProperty("merlot:creationDate")
    private StringTypeValue creationDate;

    // mandatory
    @JsonProperty("merlot:dataAccessType")
    private StringTypeValue dataAccessType;

    // mandatory
    @JsonProperty("merlot:attachments")
    private StringTypeValue attachments;

    // mandatory
    @JsonProperty("merlot:exampleCosts")
    private StringTypeValue exampleCosts;


}
