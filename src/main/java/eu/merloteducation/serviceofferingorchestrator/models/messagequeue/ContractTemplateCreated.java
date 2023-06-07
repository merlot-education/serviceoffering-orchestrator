package eu.merloteducation.serviceofferingorchestrator.models.messagequeue;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@ToString
public class ContractTemplateCreated {
    @NotNull
    private String contractId;

    @NotNull
    private String serviceOfferingId;

}
