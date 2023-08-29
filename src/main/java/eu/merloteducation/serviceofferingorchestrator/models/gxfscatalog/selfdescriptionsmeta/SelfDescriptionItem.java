package eu.merloteducation.serviceofferingorchestrator.models.gxfscatalog.selfdescriptionsmeta;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@JsonIgnoreProperties(value = {"content"})
public class SelfDescriptionItem {
    private SelfDescriptionMeta meta;
}
