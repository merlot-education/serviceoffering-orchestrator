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
    private String name;
    private String creationDate;
    private String offeredBy;
    private String merlotState;
    private String type;

    public ServiceOfferingBasicModel(SelfDescriptionItem sdItem, ServiceOfferingExtension serviceOfferingExtension) {
        SelfDescriptionMeta meta = sdItem.getMeta();
        this.id = meta.getId();
        this.sdHash = meta.getSdHash();

        ServiceOfferingCredentialSubject credentialSubject = meta.getContent()
                .getVerifiableCredential().getCredentialSubject();
        this.type = credentialSubject.getType();
        this.name = credentialSubject.getName().getValue();
        this.offeredBy = credentialSubject.getOfferedBy().getId();
        this.creationDate = credentialSubject.getCreationDate().getValue();

        if (serviceOfferingExtension != null)
            this.merlotState = serviceOfferingExtension.getState().name();


    }
}
