package eu.merloteducation.serviceofferingorchestrator.models.orchestrator;

import eu.merloteducation.serviceofferingorchestrator.models.entities.ServiceOfferingExtension;
import eu.merloteducation.serviceofferingorchestrator.models.gxfscatalog.selfdescriptions.serviceoffering.DataDeliveryCredentialSubject;
import eu.merloteducation.serviceofferingorchestrator.models.gxfscatalog.selfdescriptions.serviceoffering.SaaSCredentialSubject;
import eu.merloteducation.serviceofferingorchestrator.models.gxfscatalog.selfdescriptionsmeta.SelfDescriptionItem;

public class ServiceOfferingDetailedModelFactory {

    public static ServiceOfferingDetailedModel createServiceOfferingDetailedModel
            (SelfDescriptionItem sdItem,
             ServiceOfferingExtension serviceOfferingExtension) {
        if (sdItem.getMeta().getContent().getVerifiableCredential().getCredentialSubject()
                instanceof SaaSCredentialSubject) {
            return new SaasServiceOfferingDetailedModel(sdItem, serviceOfferingExtension);
        } else if (sdItem.getMeta().getContent().getVerifiableCredential().getCredentialSubject()
                instanceof DataDeliveryCredentialSubject) {
            return new DataDeliveryServiceOfferingDetailedModel(sdItem, serviceOfferingExtension);
        } else {
            throw new IllegalArgumentException("Credential Subject is of no known type");
        }
    }
}
