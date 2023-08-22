package eu.merloteducation.serviceofferingorchestrator.models.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ServiceOfferingBasicDto {
    private String id;
    private String type;
    private String state;
    private String name;
    private String creationDate;
    private String providerLegalName;
}
