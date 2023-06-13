package eu.merloteducation.serviceofferingorchestrator.models.orchestrator;

import eu.merloteducation.serviceofferingorchestrator.models.entities.ServiceOfferingExtension;
import eu.merloteducation.serviceofferingorchestrator.models.gxfscatalog.selfdescriptions.serviceoffering.DataDeliveryCredentialSubject;
import eu.merloteducation.serviceofferingorchestrator.models.gxfscatalog.selfdescriptions.serviceoffering.ServiceOfferingCredentialSubject;
import eu.merloteducation.serviceofferingorchestrator.models.gxfscatalog.selfdescriptionsmeta.SelfDescriptionItem;
import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
public class DataDeliveryServiceOfferingDetailedModel extends ServiceOfferingDetailedModel {

    private List<DataExchangeCount> exchangeCountOption;

    public DataDeliveryServiceOfferingDetailedModel(SelfDescriptionItem sdItem,
                                                    ServiceOfferingExtension serviceOfferingExtension) {
        super(sdItem, serviceOfferingExtension);
        ServiceOfferingCredentialSubject credentialSubject = sdItem.getMeta().getContent()
                .getVerifiableCredential().getCredentialSubject();
        if (credentialSubject instanceof DataDeliveryCredentialSubject sub) {
            this.exchangeCountOption = new ArrayList<>();
            if (sub.getExchangeCountOption() != null) {
                for (eu.merloteducation.serviceofferingorchestrator.models.gxfscatalog.DataExchangeCount dec
                        : sub.getExchangeCountOption()) {
                    DataExchangeCount decEntry = new DataExchangeCount();
                    if (dec.getExchangeCountUpTo() != null)
                        decEntry.setExchangeCountUpTo(dec.getExchangeCountUpTo().getValue());
                    decEntry.setExchangeCountUnlimited(dec.isExchangeCountUnlimited());
                    this.exchangeCountOption.add(decEntry);
                }
            }
        }
    }
}
