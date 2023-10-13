package eu.merloteducation.serviceofferingorchestrator.models.gxfscatalog;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotNull;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class TermsAndConditions {

    @NotNull
    @JsonProperty("gax-trust-framework:content")
    private StringTypeValue content;

    @NotNull
    @JsonProperty("gax-trust-framework:hash")
    private StringTypeValue hash;

    @JsonProperty("@type")
    private String type;

    @Override
    public boolean equals(Object other) {
        if (other instanceof TermsAndConditions otherTermsAndConditions){
            return content.getValue().equals((otherTermsAndConditions).getContent().getValue())
                    && hash.getValue().equals((otherTermsAndConditions).getHash().getValue());
        }
        return false;
    }

    @Override
    public int hashCode() {
        int result = 17;
        result = 31 * result + content.getValue().hashCode();
        result = 31 * result + hash.getValue().hashCode();
        return result;
    }

}
