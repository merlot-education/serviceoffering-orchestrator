package eu.merloteducation.serviceofferingorchestrator.models.orchestrator;

import eu.merloteducation.serviceofferingorchestrator.models.entities.ServiceOfferingExtension;
import eu.merloteducation.serviceofferingorchestrator.models.gxfscatalog.selfdescriptions.serviceoffering.SaaSCredentialSubject;
import eu.merloteducation.serviceofferingorchestrator.models.gxfscatalog.selfdescriptions.serviceoffering.ServiceOfferingCredentialSubject;
import eu.merloteducation.serviceofferingorchestrator.models.gxfscatalog.selfdescriptionsmeta.SelfDescriptionItem;
import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
public class SaasServiceOfferingDetailedModel extends ServiceOfferingDetailedModel {

    private String hardwareRequirements;
    private List<AllowedUserCount> userCountOption;

    public SaasServiceOfferingDetailedModel(SelfDescriptionItem sdItem,
                                            ServiceOfferingExtension serviceOfferingExtension) {
        super(sdItem, serviceOfferingExtension);
        ServiceOfferingCredentialSubject credentialSubject = sdItem.getMeta().getContent()
                .getVerifiableCredential().getCredentialSubject();
        if (credentialSubject instanceof SaaSCredentialSubject sub) {
            if (sub.getHardwareRequirements() != null)
                this.hardwareRequirements = sub.getHardwareRequirements().getValue();
            this.userCountOption = new ArrayList<>();
            if (sub.getUserCountOption() != null) {
                for (eu.merloteducation.serviceofferingorchestrator.models.gxfscatalog.AllowedUserCount uc
                        : sub.getUserCountOption()) {
                    AllowedUserCount ucEntry = new AllowedUserCount();
                    if (uc.getUserCountUpTo() != null)
                        ucEntry.setUserCountUpTo(uc.getUserCountUpTo().getValue());
                    ucEntry.setUserCountUnlimited(uc.isUserCountUnlimited());
                    this.userCountOption.add(ucEntry);
                }
            }
        }
    }
}
