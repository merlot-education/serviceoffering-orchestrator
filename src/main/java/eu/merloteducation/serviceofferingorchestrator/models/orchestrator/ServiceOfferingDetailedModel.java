package eu.merloteducation.serviceofferingorchestrator.models.orchestrator;

import eu.merloteducation.serviceofferingorchestrator.models.entities.ServiceOfferingExtension;
import eu.merloteducation.serviceofferingorchestrator.models.gxfscatalog.selfdescriptions.serviceoffering.DataDeliveryCredentialSubject;
import eu.merloteducation.serviceofferingorchestrator.models.gxfscatalog.selfdescriptions.serviceoffering.SaaSCredentialSubject;
import eu.merloteducation.serviceofferingorchestrator.models.gxfscatalog.selfdescriptions.serviceoffering.ServiceOfferingCredentialSubject;
import eu.merloteducation.serviceofferingorchestrator.models.gxfscatalog.selfdescriptionsmeta.SelfDescriptionItem;
import eu.merloteducation.serviceofferingorchestrator.models.gxfscatalog.selfdescriptionsmeta.SelfDescriptionMeta;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ServiceOfferingDetailedModel<T extends ServiceOfferingCredentialSubject> extends ServiceOfferingBasicModel {

    private String description;
    private String modifiedDate;
    private String dataAccessType;
    private String exampleCosts;
    private String attachments;
    private TermsAndConditions termsAndConditions;
    private Runtime runtime;


    private String hardwareRequirements;
    private AllowedUserCount allowedUserCount;

    private DataExchangeCount dataExchangeCount;


    public ServiceOfferingDetailedModel(SelfDescriptionItem<T> sdItem, ServiceOfferingExtension serviceOfferingExtension) {
        super((SelfDescriptionItem<ServiceOfferingCredentialSubject>)sdItem, serviceOfferingExtension);

        SelfDescriptionMeta<T> meta = sdItem.getMeta();
        this.modifiedDate = meta.getStatusDatetime();

        ServiceOfferingCredentialSubject credentialSubject = meta.getContent()
                .getVerifiableCredential().getCredentialSubject();

        if (credentialSubject.getDescription() != null)
            this.description = credentialSubject.getDescription().getValue();
        this.dataAccessType = credentialSubject.getDataAccessType().getValue();
        if (credentialSubject.getExampleCosts() != null)
            this.exampleCosts = credentialSubject.getExampleCosts().getValue();
        if (credentialSubject.getAttachments() != null)
            this.attachments = credentialSubject.getAttachments().getValue();
        this.termsAndConditions = new TermsAndConditions();
        this.termsAndConditions.setUrl(credentialSubject.getTermsAndConditions().getContent().getValue());
        this.termsAndConditions.setHash(credentialSubject.getTermsAndConditions().getHash().getValue());
        this.runtime = new Runtime();
        if (credentialSubject.getRuntimes().getRuntimeCount() != null)
            this.runtime.setRuntimeCount(credentialSubject.getRuntimes().getRuntimeCount().getValue());
        if (credentialSubject.getRuntimes().getRuntimeMeasurement() != null)
            this.runtime.setRuntimeMeasurement(credentialSubject.getRuntimes().getRuntimeMeasurement().getValue());
        this.runtime.setRuntimeUnlimited(credentialSubject.getRuntimes().isRuntimeUnlimited());


        if (credentialSubject instanceof SaaSCredentialSubject sub) {
            if (sub.getHardwareRequirements() != null)
                this.hardwareRequirements = sub.getHardwareRequirements().getValue();
            this.allowedUserCount = new AllowedUserCount();
            if (sub.getUserCountOption() != null) {
                if (sub.getUserCountOption().getUserCountUpTo() != null)
                    this.allowedUserCount.setUserCountUpTo(sub.getUserCountOption().getUserCountUpTo().getValue());
                this.allowedUserCount.setUserCountUnlimited(sub.getUserCountOption().isUserCountUnlimited());
            }
        }

        if (credentialSubject instanceof DataDeliveryCredentialSubject sub) {
            this.dataExchangeCount = new DataExchangeCount();
            if (sub.getExchangeCountOption() != null) {
                if (sub.getExchangeCountOption().getExchangeCountUpTo() != null)
                    this.dataExchangeCount.setExchangeCountUpTo(sub.getExchangeCountOption().getExchangeCountUpTo().getValue());
                this.dataExchangeCount.setExchangeCountUnlimited(sub.getExchangeCountOption().isExchangeCountUnlimited());
            }
        }




    }
}
