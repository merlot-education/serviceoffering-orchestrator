package eu.merloteducation.serviceofferingorchestrator.models.orchestrator;

import eu.merloteducation.serviceofferingorchestrator.models.entities.ServiceOfferingExtension;
import eu.merloteducation.serviceofferingorchestrator.models.gxfscatalog.selfdescriptions.serviceoffering.CooperationCredentialSubject;
import eu.merloteducation.serviceofferingorchestrator.models.gxfscatalog.selfdescriptions.serviceoffering.DataDeliveryCredentialSubject;
import eu.merloteducation.serviceofferingorchestrator.models.gxfscatalog.selfdescriptions.serviceoffering.SaaSCredentialSubject;
import eu.merloteducation.serviceofferingorchestrator.models.gxfscatalog.selfdescriptions.serviceoffering.ServiceOfferingCredentialSubject;
import eu.merloteducation.serviceofferingorchestrator.models.gxfscatalog.selfdescriptionsmeta.SelfDescriptionItem;

public class ServiceOfferingDetailedModelFactory {

    public static ServiceOfferingDetailedModel createServiceOfferingDetailedModel
            (SelfDescriptionItem sdItem,
             ServiceOfferingExtension serviceOfferingExtension) {
        ServiceOfferingCredentialSubject credentialSubject =
                sdItem.getMeta().getContent().getVerifiableCredential().getCredentialSubject();

        if (credentialSubject instanceof SaaSCredentialSubject) {
            return new SaasServiceOfferingDetailedModel(sdItem, serviceOfferingExtension);
        } else if (credentialSubject instanceof DataDeliveryCredentialSubject) {
            return new DataDeliveryServiceOfferingDetailedModel(sdItem, serviceOfferingExtension);
        } else if (credentialSubject instanceof CooperationCredentialSubject){
            return new ServiceOfferingDetailedModel(sdItem, serviceOfferingExtension);
        } else {
            throw new IllegalArgumentException("Self-Description matches no known type.");
        }
    }
}
