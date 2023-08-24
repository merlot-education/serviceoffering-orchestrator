package eu.merloteducation.serviceofferingorchestrator.service;

import com.fasterxml.jackson.databind.JsonNode;
import org.apache.commons.text.StringEscapeUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.List;

@Service
public class KeycloakAuthService {
    private final WebClient webClient;
    private final String keycloakTokenUri;
    private final String keycloakLogoutUri;
    private final String clientId;
    private final String clientSecret;
    private final String grantType;
    private final String keycloakGXFScatalogUser;
    private final String keycloakGXFScatalogPass;

    private String authToken;
    private String refreshToken;

    public KeycloakAuthService(WebClient.Builder webClientBuilder,
                               @Value("${keycloak.token-uri}") String keycloakTokenUri,
                               @Value("${keycloak.logout-uri}") String keycloakLogoutUri,
                               @Value("${keycloak.client-id}") String clientId,
                               @Value("${keycloak.client-secret}") String clientSecret,
                               @Value("${keycloak.authorization-grant-type}") String grantType,
                               @Value("${keycloak.gxfscatalog-user}") String keycloakGXFScatalogUser,
                               @Value("${keycloak.gxfscatalog-pass}") String keycloakGXFScatalogPass) {
        this.keycloakTokenUri = keycloakTokenUri;
        this.keycloakLogoutUri = keycloakLogoutUri;
        this.clientId = clientId;
        this.clientSecret = clientSecret;
        this.grantType = grantType;
        this.keycloakGXFScatalogUser = keycloakGXFScatalogUser;
        this.keycloakGXFScatalogPass = keycloakGXFScatalogPass;
        this.webClient = webClientBuilder.baseUrl("").build();
        this.loginAsGXFSCatalog();
    }

    @Scheduled(fixedDelay = 120 * 1000)
    private void scheduledLogin() {
        // TODO compute delay dynamically from token
        loginAsGXFSCatalog();
    }

    private void loginAsGXFSCatalog() {
        MultiValueMap<String, String> map = new LinkedMultiValueMap<>();
        map.add("username", keycloakGXFScatalogUser);
        map.add("password", keycloakGXFScatalogPass);
        map.add("client_id", clientId);
        map.add("client_secret", clientSecret);
        map.add("grant_type", grantType);
        HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<>(map, new HttpHeaders());
        JsonNode loginResult = webClient.post()
                .uri(keycloakTokenUri)
                .body(BodyInserters.fromValue(map))
                .retrieve()
                .bodyToMono(JsonNode.class)
                .block();
        if (loginResult != null) {
            if (loginResult.has("access_token")) {
                String previousRefreshToken = this.refreshToken;
                // store new tokens
                this.authToken = loginResult.get("access_token").asText();
                this.refreshToken = loginResult.get("refresh_token").asText();
                // end previous session
                logoutAsGXFSCatalog(previousRefreshToken);

            }
        }
    }

    private void logoutAsGXFSCatalog(String refreshToken) {
        if (refreshToken == null) {
            return;
        }

        MultiValueMap<String, String> map = new LinkedMultiValueMap<>();
        map.add("client_id", clientId);
        map.add("client_secret", clientSecret);
        map.add("refresh_token", refreshToken);

        webClient.post()
                .uri(keycloakLogoutUri)
                .body(BodyInserters.fromValue(map))
                .retrieve().toBodilessEntity().subscribe();
    }

    public String webCallAuthenticated(HttpMethod method, String uri, String body, List<MediaType> mediaTypes) {
        String response = webClient.method(method)
                .uri(uri)
                .headers(h -> {
                    h.setBearerAuth(this.authToken);
                    if (mediaTypes != null) {
                        h.setAccept(mediaTypes);
                    }
                })
                .body(BodyInserters.fromValue(body))
                .retrieve()
                .bodyToMono(String.class).block();

        response = StringEscapeUtils.unescapeJson(response);
        if (response != null)
            response = response.replace("\"{", "{").replace("}\"", "}");
        return response;
    }

}
