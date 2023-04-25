package eu.merloteducation.serviceofferingorchestrator.models.orchestrator;

import eu.merloteducation.serviceofferingorchestrator.models.gxfscatalog.selfdescriptions.SelfDescription;
import eu.merloteducation.serviceofferingorchestrator.models.gxfscatalog.selfdescriptions.serviceoffering.ServiceOfferingCredentialSubject;
import eu.merloteducation.serviceofferingorchestrator.models.gxfscatalog.selfdescriptionsmeta.SelfDescriptionItem;
import eu.merloteducation.serviceofferingorchestrator.models.gxfscatalog.selfdescriptionsmeta.SelfDescriptionMeta;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ServiceOfferingModel {
    private String id;
    private String sdHash;
    private String status;
    private String issuer;
    private String uploadTime;
    private String statusTime;

    private String type;
    private String termsAndConditionsHash;
    private String termsAndConditionsUrl;
    private String providedBy;
    private String policy;
    private String name;
    private String dataAccountExportFormatType;
    private String dataAccountExportAccessType;
    private String dataAccountExportRequestType;
    private String offeredBy;
    private String endPointUrl;


    public ServiceOfferingModel(SelfDescriptionItem sdItem, SelfDescription sd) {
        SelfDescriptionMeta meta = sdItem.getMeta();
        this.id = meta.getId();
        this.sdHash = meta.getSdHash();
        this.status = meta.getStatus();
        this.issuer = meta.getIssuer();
        this.uploadTime = meta.getUploadDatetime();
        this.statusTime = meta.getStatusDatetime();

        ServiceOfferingCredentialSubject credentialSubject = sd.getVerifiableCredential().getCredentialSubject();
        this.type = credentialSubject.getType();
        this.termsAndConditionsHash = credentialSubject.getTermsAndConditions().getHash().getValue();
        this.termsAndConditionsUrl = credentialSubject.getTermsAndConditions().getContent().getValue();
        this.providedBy = credentialSubject.getProvidedBy().getId();
        this.policy = credentialSubject.getPolicy().getValue();
        this.name = credentialSubject.getName().getValue();
        this.dataAccountExportFormatType = credentialSubject.getDataAccountExport().getFormatType().getValue();
        this.dataAccountExportAccessType = credentialSubject.getDataAccountExport().getAccessType().getValue();
        this.dataAccountExportRequestType = credentialSubject.getDataAccountExport().getRequestType().getValue();
        this.offeredBy = credentialSubject.getOfferedBy().getId();
        this.endPointUrl = credentialSubject.getEndpoint().getEndPointURL().getValue();

    }
}
