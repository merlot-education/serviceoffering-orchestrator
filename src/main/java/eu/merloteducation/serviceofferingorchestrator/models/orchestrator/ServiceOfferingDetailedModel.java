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
public class ServiceOfferingDetailedModel extends ServiceOfferingBasicModel {

    private String description;
    private String modifiedDate;
    private String dataAccessType;
    private String exampleCosts;
    private List<String> attachments;
    private List<TermsAndConditions> termsAndConditions;
    private List<Runtime> runtimeOption;
    private boolean merlotTermsAndConditionsAccepted;


    public ServiceOfferingDetailedModel(SelfDescriptionItem sdItem,
                                        ServiceOfferingExtension serviceOfferingExtension) {
        super(sdItem, serviceOfferingExtension);

        SelfDescriptionMeta meta = sdItem.getMeta();
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
            tncEntry.setContent(tnc.getContent().getValue());
            tncEntry.setHash(tnc.getHash().getValue());
            this.termsAndConditions.add(tncEntry);
        }
        this.runtimeOption = new ArrayList<>();
        for (eu.merloteducation.serviceofferingorchestrator.models.gxfscatalog.Runtime rt
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
    }
}
