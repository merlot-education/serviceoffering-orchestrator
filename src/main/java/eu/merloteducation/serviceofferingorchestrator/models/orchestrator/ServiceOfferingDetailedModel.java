package eu.merloteducation.serviceofferingorchestrator.models.orchestrator;

import eu.merloteducation.serviceofferingorchestrator.models.entities.ServiceOfferingExtension;
import eu.merloteducation.serviceofferingorchestrator.models.gxfscatalog.StringTypeValue;
import eu.merloteducation.serviceofferingorchestrator.models.gxfscatalog.selfdescriptions.serviceoffering.DataDeliveryCredentialSubject;
import eu.merloteducation.serviceofferingorchestrator.models.gxfscatalog.selfdescriptions.serviceoffering.SaaSCredentialSubject;
import eu.merloteducation.serviceofferingorchestrator.models.gxfscatalog.selfdescriptions.serviceoffering.ServiceOfferingCredentialSubject;
import eu.merloteducation.serviceofferingorchestrator.models.gxfscatalog.selfdescriptionsmeta.SelfDescriptionItem;
import eu.merloteducation.serviceofferingorchestrator.models.gxfscatalog.selfdescriptionsmeta.SelfDescriptionMeta;
import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Getter
@Setter
public class ServiceOfferingDetailedModel<T extends ServiceOfferingCredentialSubject> extends ServiceOfferingBasicModel {

    private String description;
    private String modifiedDate;
    private String dataAccessType;
    private String exampleCosts;
    private List<String> attachments;
    private List<TermsAndConditions> termsAndConditions;
    private List<Runtime> runtimes;


    private String hardwareRequirements;
    private List<AllowedUserCount> allowedUserCounts;

    private List<DataExchangeCount> dataExchangeCounts;


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
            this.attachments = credentialSubject.getAttachments().stream().map(StringTypeValue::getValue).collect(Collectors.toList());
        this.termsAndConditions = new ArrayList<>();
        for (eu.merloteducation.serviceofferingorchestrator.models.gxfscatalog.TermsAndConditions tnc
                : credentialSubject.getTermsAndConditions()) {
            TermsAndConditions tncEntry = new TermsAndConditions();
            tncEntry.setUrl(tnc.getContent().getValue());
            tncEntry.setHash(tnc.getHash().getValue());
            this.termsAndConditions.add(tncEntry);
        }
        this.runtimes = new ArrayList<>();
        for (eu.merloteducation.serviceofferingorchestrator.models.gxfscatalog.Runtime rt
                : credentialSubject.getRuntimes()) {
            Runtime rtEntry = new Runtime();
            if (rt.getRuntimeCount() != null)
                rtEntry.setRuntimeCount(rt.getRuntimeCount().getValue());
            if (rt.getRuntimeMeasurement() != null)
                rtEntry.setRuntimeMeasurement(rt.getRuntimeMeasurement().getValue());
            rtEntry.setRuntimeUnlimited(rt.isRuntimeUnlimited());
            this.runtimes.add(rtEntry);
        }

        if (credentialSubject instanceof SaaSCredentialSubject sub) {
            if (sub.getHardwareRequirements() != null)
                this.hardwareRequirements = sub.getHardwareRequirements().getValue();
            this.allowedUserCounts = new ArrayList<>();
            if (sub.getUserCountOption() != null) {
                for (eu.merloteducation.serviceofferingorchestrator.models.gxfscatalog.AllowedUserCount uc
                        : sub.getUserCountOption()) {
                    AllowedUserCount ucEntry = new AllowedUserCount();
                    if (uc.getUserCountUpTo() != null)
                        ucEntry.setUserCountUpTo(uc.getUserCountUpTo().getValue());
                    ucEntry.setUserCountUnlimited(uc.isUserCountUnlimited());
                    this.allowedUserCounts.add(ucEntry);
                }
            }
        }

        if (credentialSubject instanceof DataDeliveryCredentialSubject sub) {
            this.dataExchangeCounts = new ArrayList<>();
            if (sub.getExchangeCountOption() != null) {
                for (eu.merloteducation.serviceofferingorchestrator.models.gxfscatalog.DataExchangeCount dec
                        : sub.getExchangeCountOption()) {
                    DataExchangeCount decEntry = new DataExchangeCount();
                    if (dec.getExchangeCountUpTo() != null)
                        decEntry.setExchangeCountUpTo(dec.getExchangeCountUpTo().getValue());
                    decEntry.setExchangeCountUnlimited(dec.isExchangeCountUnlimited());
                    this.dataExchangeCounts.add(decEntry);
                }
            }
        }




    }
}
