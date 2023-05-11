package eu.merloteducation.serviceofferingorchestrator.models.orchestrator;

import eu.merloteducation.serviceofferingorchestrator.models.entities.ServiceOfferingExtension;
import eu.merloteducation.serviceofferingorchestrator.models.gxfscatalog.selfdescriptions.SelfDescription;
import eu.merloteducation.serviceofferingorchestrator.models.gxfscatalog.selfdescriptions.serviceoffering.ServiceOfferingCredentialSubject;
import eu.merloteducation.serviceofferingorchestrator.models.gxfscatalog.selfdescriptionsmeta.SelfDescriptionItem;
import eu.merloteducation.serviceofferingorchestrator.models.gxfscatalog.selfdescriptionsmeta.SelfDescriptionMeta;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ServiceOfferingBasicModel{
    private String id;
    private String sdHash;
    private String status;
    private String merlotState;
    private String issuer;
    private String uploadTime;
    private String statusTime;

    private String type;
    private String offeredBy;
    private String providedBy;
    private String name;
    private String description;

    public ServiceOfferingBasicModel(SelfDescriptionItem sdItem, ServiceOfferingExtension serviceOfferingExtension) {
        SelfDescriptionMeta meta = sdItem.getMeta();
        this.id = meta.getId();
        this.sdHash = meta.getSdHash();
        this.status = meta.getStatus();
        this.issuer = meta.getIssuer();
        this.uploadTime = meta.getUploadDatetime();
        this.statusTime = meta.getStatusDatetime();

        ServiceOfferingCredentialSubject credentialSubject = meta.getContent()
                .getVerifiableCredential().getCredentialSubject();
        this.type = credentialSubject.getType();
        this.providedBy = credentialSubject.getProvidedBy().getId();
        this.name = credentialSubject.getName().getValue();
        this.offeredBy = credentialSubject.getOfferedBy().getId();
        if (credentialSubject.getDescription() != null)
            this.description = credentialSubject.getDescription().getValue();

        if (serviceOfferingExtension != null)
            this.merlotState = serviceOfferingExtension.getState().name();

    }
}
