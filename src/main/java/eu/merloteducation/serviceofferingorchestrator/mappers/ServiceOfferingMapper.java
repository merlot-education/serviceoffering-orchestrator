package eu.merloteducation.serviceofferingorchestrator.mappers;

import eu.merloteducation.gxfscataloglibrary.models.selfdescriptions.SelfDescriptionMeta;
import eu.merloteducation.gxfscataloglibrary.models.selfdescriptions.gx.serviceofferings.ServiceOfferingCredentialSubject;
import eu.merloteducation.gxfscataloglibrary.models.selfdescriptions.merlot.participants.MerlotLegalParticipantCredentialSubject;
import eu.merloteducation.modelslib.api.organization.MerlotParticipantDto;
import eu.merloteducation.modelslib.api.serviceoffering.ProviderDetailsDto;
import eu.merloteducation.modelslib.api.serviceoffering.ServiceOfferingBasicDto;
import eu.merloteducation.modelslib.api.serviceoffering.ServiceOfferingDto;
import eu.merloteducation.serviceofferingorchestrator.models.entities.ServiceOfferingExtension;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;

import java.time.OffsetDateTime;

@Mapper(componentModel = "spring", imports = { MerlotLegalParticipantCredentialSubject.class })
public interface ServiceOfferingMapper {

    default String getDateTimeString(OffsetDateTime offsetDateTime) {
        return offsetDateTime != null ? offsetDateTime.toString() : null;
    }

    @Named("providerDetailsDtoMap")
    default ProviderDetailsDto providerDetailsDtoMap(MerlotParticipantDto providerDetails) {
        MerlotLegalParticipantCredentialSubject cs = providerDetails.getSelfDescription()
                .findFirstCredentialSubjectByType(MerlotLegalParticipantCredentialSubject.class);
        ProviderDetailsDto providerDetailsDto = new ProviderDetailsDto();

        providerDetailsDto.setProviderId(cs.getId());
        providerDetailsDto.setProviderLegalName(cs.getLegalName());
        return providerDetailsDto;
    }

    @Named("providerDetailsToLegalName")
    default String providerDetailsToLegalName(MerlotParticipantDto providerDetails) {
        return providerDetails.getSelfDescription()
                .findFirstCredentialSubjectByType(MerlotLegalParticipantCredentialSubject.class)
                .getLegalName();
    }

    default ServiceOfferingBasicDto selfDescriptionMetaToServiceOfferingBasicDto(SelfDescriptionMeta selfDescriptionMeta,
                                                                                 ServiceOfferingExtension extension,
                                                                                 MerlotParticipantDto providerDetails) {
        MerlotLegalParticipantCredentialSubject participantCs = providerDetails.getSelfDescription()
                .findFirstCredentialSubjectByType(MerlotLegalParticipantCredentialSubject.class);
        ServiceOfferingCredentialSubject offeringCs = selfDescriptionMeta.getContent()
                .findFirstCredentialSubjectByType(ServiceOfferingCredentialSubject.class);

        ServiceOfferingBasicDto dto = new ServiceOfferingBasicDto();
        dto.setId(offeringCs.getId());
        dto.setType("merlot:MerlotServiceOffering"); // TODO change depending on specific type
        dto.setState(extension.getState().toString());
        dto.setName(offeringCs.getName());
        dto.setCreationDate(getDateTimeString(extension.getCreationDate()));
        dto.setProviderLegalName(participantCs.getLegalName());

        return dto;
    }

    @Mapping(target = "metadata.state", source = "extension.state")
    @Mapping(target = "metadata.hash", source = "extension.currentSdHash")
    @Mapping(target = "metadata.creationDate", source = "extension.creationDate")
    @Mapping(target = "metadata.modifiedDate", source = "selfDescriptionMeta.statusDatetime")
    @Mapping(target = "metadata.signedBy", source = "signerLegalName")
    @Mapping(target = "selfDescription", source = "selfDescriptionMeta.content")
    @Mapping(target = "providerDetails", source = "providerDetails", qualifiedByName = "providerDetailsDtoMap")
    ServiceOfferingDto selfDescriptionMetaToServiceOfferingDto(SelfDescriptionMeta selfDescriptionMeta,
                                                               ServiceOfferingExtension extension,
                                                               MerlotParticipantDto providerDetails,
                                                               String signerLegalName);
}
