package eu.merloteducation.serviceofferingorchestrator.mappers;

import eu.merloteducation.gxfscataloglibrary.models.selfdescriptions.SelfDescriptionMeta;
import eu.merloteducation.gxfscataloglibrary.models.selfdescriptions.merlot.participants.MerlotOrganizationCredentialSubject;
import eu.merloteducation.gxfscataloglibrary.models.selfdescriptions.merlot.serviceofferings.MerlotServiceOfferingCredentialSubject;
import eu.merloteducation.modelslib.api.organization.MerlotParticipantDto;
import eu.merloteducation.modelslib.api.serviceoffering.ProviderDetailsDto;
import eu.merloteducation.modelslib.api.serviceoffering.ServiceOfferingBasicDto;
import eu.merloteducation.modelslib.api.serviceoffering.ServiceOfferingDto;
import eu.merloteducation.serviceofferingorchestrator.models.entities.ServiceOfferingExtension;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;

import java.time.OffsetDateTime;

@Mapper(componentModel = "spring", imports = { MerlotServiceOfferingCredentialSubject.class, MerlotOrganizationCredentialSubject.class })
public interface ServiceOfferingMapper {

    default String map(OffsetDateTime offsetDateTime) {
        return offsetDateTime != null ? offsetDateTime.toString() : null;
    }

    @Named("providerDetailsDtoMap")
    default ProviderDetailsDto providerDetailsDtoMap(MerlotParticipantDto providerDetails) {
        ProviderDetailsDto providerDetailsDto = new ProviderDetailsDto();
        MerlotOrganizationCredentialSubject credentialSubject =
                (MerlotOrganizationCredentialSubject) providerDetails.getSelfDescription()
                        .getVerifiableCredential().getCredentialSubject();

        providerDetailsDto.setProviderId(credentialSubject.getId());
        providerDetailsDto.setProviderLegalName(credentialSubject.getLegalName());
        return providerDetailsDto;
    }

    @Mapping(target = "id", source = "selfDescriptionMeta.content.verifiableCredential.credentialSubject.id")
    @Mapping(target = "type", source = "selfDescriptionMeta.content.verifiableCredential.credentialSubject.type")
    @Mapping(target = "state", source = "extension.state")
    @Mapping(target = "name", expression = "java(((MerlotServiceOfferingCredentialSubject) selfDescriptionMeta.getContent().getVerifiableCredential().getCredentialSubject()).getName())")
    @Mapping(target = "creationDate", source = "extension.creationDate")
    @Mapping(target = "providerLegalName", expression = "java(((MerlotOrganizationCredentialSubject) providerDetails.getSelfDescription().getVerifiableCredential().getCredentialSubject()).getLegalName())")
    ServiceOfferingBasicDto selfDescriptionMetaToServiceOfferingBasicDto(SelfDescriptionMeta selfDescriptionMeta,
                                                                         ServiceOfferingExtension extension,
                                                                         MerlotParticipantDto providerDetails);
    @Mapping(target = "metadata.state", source = "extension.state")
    @Mapping(target = "metadata.hash", source = "extension.currentSdHash")
    @Mapping(target = "metadata.creationDate", source = "extension.creationDate")
    @Mapping(target = "metadata.modifiedDate", source = "selfDescriptionMeta.statusDatetime")
    @Mapping(target = "selfDescription", source = "selfDescriptionMeta.content")
    @Mapping(target = "providerDetails", source = "providerDetails", qualifiedByName = "providerDetailsDtoMap")
    ServiceOfferingDto selfDescriptionMetaToServiceOfferingDto(SelfDescriptionMeta selfDescriptionMeta,
                                                               ServiceOfferingExtension extension,
                                                               MerlotParticipantDto providerDetails);
}
