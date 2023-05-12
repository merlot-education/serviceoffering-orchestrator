package eu.merloteducation.serviceofferingorchestrator.models.orchestrator;

import eu.merloteducation.serviceofferingorchestrator.models.entities.ServiceOfferingExtension;
import eu.merloteducation.serviceofferingorchestrator.models.gxfscatalog.selfdescriptions.serviceoffering.ServiceOfferingCredentialSubject;
import eu.merloteducation.serviceofferingorchestrator.models.gxfscatalog.selfdescriptionsmeta.SelfDescriptionItem;
import eu.merloteducation.serviceofferingorchestrator.models.gxfscatalog.selfdescriptionsmeta.SelfDescriptionMeta;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ServiceOfferingDetailedModel extends ServiceOfferingBasicModel {

    private String status;
    private String issuer;
    private String uploadTime;
    private String statusTime;

    private String providedBy;
    private String description;

    private String termsAndConditionsHash;
    private String termsAndConditionsUrl;
    private String policy;
    private String dataAccountExportFormatType;
    private String dataAccountExportAccessType;
    private String dataAccountExportRequestType;
    private String endPointUrl;


    public ServiceOfferingDetailedModel(SelfDescriptionItem sdItem, ServiceOfferingExtension serviceOfferingExtension) {
        super(sdItem, serviceOfferingExtension);

        SelfDescriptionMeta meta = sdItem.getMeta();
        this.status = meta.getStatus();
        this.issuer = meta.getIssuer();
        this.uploadTime = meta.getUploadDatetime();
        this.statusTime = meta.getStatusDatetime();

        ServiceOfferingCredentialSubject credentialSubject = meta.getContent()
                .getVerifiableCredential().getCredentialSubject();
        this.providedBy = credentialSubject.getProvidedBy().getId();
        if (credentialSubject.getDescription() != null)
            this.description = credentialSubject.getDescription().getValue();

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
