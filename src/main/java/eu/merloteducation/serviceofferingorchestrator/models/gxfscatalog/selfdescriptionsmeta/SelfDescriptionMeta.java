package eu.merloteducation.serviceofferingorchestrator.models.gxfscatalog.selfdescriptionsmeta;

import eu.merloteducation.serviceofferingorchestrator.models.gxfscatalog.selfdescriptions.SelfDescription;
import eu.merloteducation.serviceofferingorchestrator.models.gxfscatalog.selfdescriptions.serviceoffering.ServiceOfferingCredentialSubject;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class SelfDescriptionMeta {
    private String expirationTime;
    private SelfDescription content;
    private List<String> validators;
    private String subjectId;
    private String sdHash;
    private String id;
    private String status;
    private String issuer;
    private List<String> validatorDids;
    private String uploadDatetime;
    private String statusDatetime;
}
