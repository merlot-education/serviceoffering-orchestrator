package eu.merloteducation.serviceofferingorchestrator.models.orchestrator;

import eu.merloteducation.serviceofferingorchestrator.models.gxfscatalog.selfdescriptions.serviceoffering.ServiceOfferingCredentialSubject;
import eu.merloteducation.serviceofferingorchestrator.models.gxfscatalog.selfdescriptionsmeta.SelfDescriptionItem;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ServiceOfferingDetailedModel extends ServiceOfferingBasicModel {

    private String termsAndConditionsHash;
    private String termsAndConditionsUrl;
    private String policy;
    private String dataAccountExportFormatType;
    private String dataAccountExportAccessType;
    private String dataAccountExportRequestType;
    private String endPointUrl;


    public ServiceOfferingDetailedModel(SelfDescriptionItem sdItem) {
        super(sdItem);

        ServiceOfferingCredentialSubject credentialSubject = sdItem.getMeta().getContent()
                .getVerifiableCredential().getCredentialSubject();

        this.termsAndConditionsHash = credentialSubject.getTermsAndConditions().getHash().getValue();
        this.termsAndConditionsUrl = credentialSubject.getTermsAndConditions().getContent().getValue();
        this.policy = credentialSubject.getPolicy().getValue();
        this.dataAccountExportFormatType = credentialSubject.getDataAccountExport().getFormatType().getValue();
        this.dataAccountExportAccessType = credentialSubject.getDataAccountExport().getAccessType().getValue();
        this.dataAccountExportRequestType = credentialSubject.getDataAccountExport().getRequestType().getValue();
        if (credentialSubject.getEndpoint() != null && credentialSubject.getEndpoint().getEndPointURL() != null)
            this.endPointUrl = credentialSubject.getEndpoint().getEndPointURL().getValue();

    }
}
