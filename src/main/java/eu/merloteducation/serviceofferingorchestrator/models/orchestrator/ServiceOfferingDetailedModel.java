package eu.merloteducation.serviceofferingorchestrator.models.orchestrator;

import eu.merloteducation.modelslib.gxfscatalog.StringTypeValue;
import eu.merloteducation.modelslib.gxfscatalog.selfdescriptions.serviceoffering.DataDeliveryCredentialSubject;
import eu.merloteducation.modelslib.gxfscatalog.selfdescriptions.serviceoffering.SaaSCredentialSubject;
import eu.merloteducation.modelslib.gxfscatalog.selfdescriptions.serviceoffering.ServiceOfferingCredentialSubject;
import eu.merloteducation.modelslib.gxfscatalog.selfdescriptionsmeta.SelfDescriptionItem;
import eu.merloteducation.modelslib.gxfscatalog.selfdescriptionsmeta.SelfDescriptionMeta;
import eu.merloteducation.serviceofferingorchestrator.models.entities.ServiceOfferingExtension;
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
    private List<Runtime> runtimeOption;
    private boolean merlotTermsAndConditionsAccepted;


    private String hardwareRequirements;
    private List<AllowedUserCount> userCountOption;

    private List<DataExchangeCount> exchangeCountOption;


    public ServiceOfferingDetailedModel(SelfDescriptionItem<T> sdItem, ServiceOfferingExtension serviceOfferingExtension) {
        super((SelfDescriptionItem<ServiceOfferingCredentialSubject>) sdItem, serviceOfferingExtension);

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
        for (eu.merloteducation.modelslib.gxfscatalog.TermsAndConditions tnc
                : credentialSubject.getTermsAndConditions()) {
            TermsAndConditions tncEntry = new TermsAndConditions();
            tncEntry.setContent(tnc.getContent().getValue());
            tncEntry.setHash(tnc.getHash().getValue());
            this.termsAndConditions.add(tncEntry);
        }
        this.runtimeOption = new ArrayList<>();
        for (eu.merloteducation.modelslib.gxfscatalog.Runtime rt
                : credentialSubject.getRuntimes()) {
            Runtime rtEntry = new Runtime();
            if (rt.getRuntimeCount() != null)
                rtEntry.setRuntimeCount(rt.getRuntimeCount().getValue());
            if (rt.getRuntimeMeasurement() != null)
                rtEntry.setRuntimeMeasurement(rt.getRuntimeMeasurement().getValue());
            rtEntry.setRuntimeUnlimited(rt.isRuntimeUnlimited());
            this.runtimeOption.add(rtEntry);
        }
        this.merlotTermsAndConditionsAccepted = credentialSubject.isMerlotTermsAndConditionsAccepted();

        if (credentialSubject instanceof SaaSCredentialSubject sub) {
            if (sub.getHardwareRequirements() != null)
                this.hardwareRequirements = sub.getHardwareRequirements().getValue();
            this.userCountOption = new ArrayList<>();
            if (sub.getUserCountOption() != null) {
                for (eu.merloteducation.modelslib.gxfscatalog.AllowedUserCount uc
                        : sub.getUserCountOption()) {
                    AllowedUserCount ucEntry = new AllowedUserCount();
                    if (uc.getUserCountUpTo() != null)
                        ucEntry.setUserCountUpTo(uc.getUserCountUpTo().getValue());
                    ucEntry.setUserCountUnlimited(uc.isUserCountUnlimited());
                    this.userCountOption.add(ucEntry);
                }
            }
        }

        if (credentialSubject instanceof DataDeliveryCredentialSubject sub) {
            this.exchangeCountOption = new ArrayList<>();
            if (sub.getExchangeCountOption() != null) {
                for (eu.merloteducation.modelslib.gxfscatalog.DataExchangeCount dec
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
