package eu.merloteducation.serviceofferingorchestrator.mappers;

import eu.merloteducation.serviceofferingorchestrator.models.dto.ServiceOfferingDto;
import eu.merloteducation.serviceofferingorchestrator.models.entities.ServiceOfferingExtension;
import eu.merloteducation.serviceofferingorchestrator.models.gxfscatalog.selfdescriptions.SelfDescription;
import eu.merloteducation.serviceofferingorchestrator.models.gxfscatalog.selfdescriptionsmeta.SelfDescriptionMeta;
import eu.merloteducation.serviceofferingorchestrator.models.organisationsorchestrator.OrganizationDetails;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.time.OffsetDateTime;

@Mapper(componentModel = "spring")
public interface ServiceOfferingMapper {

    default String map(OffsetDateTime offsetDateTime) {
        return offsetDateTime != null ? offsetDateTime.toString() : null;
    }
    @Mapping(target = "metadata.state", source = "extension.state")
    @Mapping(target = "metadata.creationDate", source = "extension.creationDate")
    @Mapping(target = "selfDescription", source = "selfDescriptionMeta.content")
    @Mapping(target = "providerDetails.providerId",
            source = "providerDetails.selfDescription.verifiableCredential.credentialSubject.id")
    @Mapping(target = "providerDetails.providerLegalName",
            source = "providerDetails.selfDescription.verifiableCredential.credentialSubject.legalName.value")
    ServiceOfferingDto selfDescriptionMetaToServiceOfferingDto(SelfDescriptionMeta selfDescriptionMeta,
                                                               ServiceOfferingExtension extension,
                                                               OrganizationDetails providerDetails);
}
