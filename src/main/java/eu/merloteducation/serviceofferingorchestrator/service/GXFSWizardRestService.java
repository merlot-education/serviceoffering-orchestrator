package eu.merloteducation.serviceofferingorchestrator.service;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import eu.merloteducation.serviceofferingorchestrator.repositories.ServiceOfferingExtensionRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;


@Service
public class GXFSWizardRestService {
    @Autowired
    private RestTemplate restTemplate;

    @Autowired
    private ServiceOfferingExtensionRepository serviceOfferingExtensionRepository;

    @Value("${gxfswizard.base-uri}")
    private String gxfswizardBaseUri;


    public Map<String, List<String>> getServiceOfferingShapes() throws Exception {
        String response =
                restTemplate.exchange(gxfswizardBaseUri + "/getAvailableShapesCategorized?ecoSystem=merlot",
                        HttpMethod.GET, null, String.class).getBody();
        ObjectMapper mapper = new ObjectMapper().configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        Map<String, List<String>> shapesResponse = mapper.readValue(response, Map.class);
        // as this list also contains organization and other shapes, we need to filter before returning it
        return shapesResponse.entrySet().stream()
                .filter(x -> x.getKey().equals("Service"))
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
    }

    public String getShape(String jsonName) throws Exception {
        if (!jsonName.matches("[A-Za-z ]*.json")) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid file name.");
        }
        try {
            return restTemplate.exchange(gxfswizardBaseUri + "/getJSON?name=" + jsonName,
                    HttpMethod.GET, null, String.class).getBody();
        } catch (HttpClientErrorException e) {
            throw new ResponseStatusException(HttpStatus.valueOf(e.getStatusCode().value()), e.getResponseBodyAsString());
        }

    }

}
