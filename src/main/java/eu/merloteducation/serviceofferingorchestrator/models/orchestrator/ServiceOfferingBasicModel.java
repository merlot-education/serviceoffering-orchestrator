package eu.merloteducation.serviceofferingorchestrator.models.orchestrator;

import eu.merloteducation.modelslib.gxfscatalog.selfdescriptions.serviceoffering.ServiceOfferingCredentialSubject;
import eu.merloteducation.modelslib.gxfscatalog.selfdescriptionsmeta.SelfDescriptionItem;
import eu.merloteducation.modelslib.gxfscatalog.selfdescriptionsmeta.SelfDescriptionMeta;
import eu.merloteducation.serviceofferingorchestrator.models.entities.ServiceOfferingExtension;
import lombok.Getter;
import lombok.Setter;

import java.time.format.DateTimeFormatter;

@Getter
@Setter
public class ServiceOfferingBasicModel {
    private String id;
    private String sdHash;
    private String name;
    private String creationDate;
    private String offeredBy;
    private String merlotState;
    private String type;

    public ServiceOfferingBasicModel(SelfDescriptionItem<ServiceOfferingCredentialSubject> sdItem, ServiceOfferingExtension serviceOfferingExtension) {
        SelfDescriptionMeta<ServiceOfferingCredentialSubject> meta = sdItem.getMeta();
        this.id = meta.getId();
        this.sdHash = meta.getSdHash();

        ServiceOfferingCredentialSubject credentialSubject = meta.getContent()
                .getVerifiableCredential().getCredentialSubject();
        this.type = credentialSubject.getType();
        this.name = credentialSubject.getName().getValue();
        this.offeredBy = credentialSubject.getOfferedBy().getId();

        if (serviceOfferingExtension != null) {
            this.merlotState = serviceOfferingExtension.getState().name();
            if (serviceOfferingExtension.getCreationDate() != null) {
                this.creationDate = serviceOfferingExtension.getCreationDate().format(DateTimeFormatter.ISO_INSTANT);
            }
        }


    }
}
