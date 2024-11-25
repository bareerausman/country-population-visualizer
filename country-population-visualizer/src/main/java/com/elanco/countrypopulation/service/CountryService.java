package com.elanco.countrypopulation.service;

import com.elanco.countrypopulation.model.Country;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.server.ResponseStatusException;

import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
public class CountryService {
    private static final String COUNTRIES_NOW_API = "https://countriesnow.space/api/v0.1/countries";

    private final RestTemplate restTemplate;

    @Autowired
    public CountryService(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    @Cacheable("countries")
    public List<Country> getAllCountries() {
        try {
            List<Country> countries = getCountriesWithCapitals();
            enrichWithFlags(countries);

            return countries.stream()
                    .filter(c -> c.getName() != null)
                    .sorted(Comparator.comparing(Country::getName))
                    .collect(Collectors.toList());
        } catch (Exception e) {
            log.error("Failed to retrieve country data", e);
            throw new ResponseStatusException(
                    HttpStatus.INTERNAL_SERVER_ERROR,
                    "Failed to retrieve country data: " + e.getMessage()
            );
        }
    }

    private List<Country> getCountriesWithCapitals() {
        try {
            String url = COUNTRIES_NOW_API + "/capital";

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);

            HttpEntity<?> entity = new HttpEntity<>(headers);

            ResponseEntity<Map> response = restTemplate.exchange(
                    url,
                    HttpMethod.GET,
                    entity,
                    Map.class
            );

            if (response.getBody() == null || !response.getBody().containsKey("data")) {
                throw new RuntimeException("No country data received");
            }

            List<Map<String, String>> countriesData =
                    (List<Map<String, String>>) response.getBody().get("data");

            return countriesData.stream()
                    .map(this::mapToCountry)
                    .collect(Collectors.toList());

        } catch (Exception e) {
            log.error("Error fetching countries with capitals", e);
            throw new RuntimeException("Failed to fetch countries with capitals", e);
        }
    }

    private Country mapToCountry(Map<String, String> data) {
        Country country = new Country();
        country.setName(data.get("name"));
        country.setCapital(data.get("capital"));
        return country;
    }

    private void enrichWithFlags(List<Country> countries) {
        try {
            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("countries",
                    countries.stream()
                            .map(Country::getName)
                            .collect(Collectors.toList())
            );

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);

            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(requestBody, headers);

            ResponseEntity<Map> response = restTemplate.exchange(
                    COUNTRIES_NOW_API + "/flag/images",
                    HttpMethod.POST,
                    entity,
                    Map.class
            );

            if (response.getBody() == null || !response.getBody().containsKey("data")) {
                log.warn("No flag data received");
                return;
            }

            List<Map<String, String>> flagData =
                    (List<Map<String, String>>) response.getBody().get("data");

            Map<String, String> flagMap = flagData.stream()
                    .collect(Collectors.toMap(
                            data -> data.get("name"),
                            data -> data.get("flag"),
                            (v1, v2) -> v1
                    ));

            countries.forEach(country ->
                    country.setFlag(flagMap.getOrDefault(country.getName(), null))
            );

        } catch (Exception e) {
            log.error("Error fetching country flags", e);
        }
    }

    @Cacheable("country")
    public Country getCountryByName(String name) {
        try {
            List<Country> countries = getAllCountries();

            return countries.stream()
                    .filter(c -> c.getName().equalsIgnoreCase(name))
                    .findFirst()
                    .orElseThrow(() -> new ResponseStatusException(
                            HttpStatus.NOT_FOUND,
                            "Country not found: " + name
                    ));

        } catch (Exception e) {
            log.error("Failed to retrieve country details", e);
            throw new ResponseStatusException(
                    HttpStatus.INTERNAL_SERVER_ERROR,
                    "Failed to retrieve country details: " + e.getMessage()
            );
        }
    }

    public List<Map<String, Object>> getCityPopulations(String countryName) {
        try {
            Map<String, String> requestBody = new HashMap<>();
            requestBody.put("country", countryName);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);

            HttpEntity<Map<String, String>> entity = new HttpEntity<>(requestBody, headers);

            ResponseEntity<Map> response = restTemplate.exchange(
                    COUNTRIES_NOW_API + "/population/cities",
                    HttpMethod.POST,
                    entity,
                    Map.class
            );

            if (response.getBody() == null || !response.getBody().containsKey("data")) {
                log.warn("No city population data received for {}", countryName);
                return Collections.emptyList();
            }

            return (List<Map<String, Object>>) response.getBody().get("data");

        } catch (Exception e) {
            log.error("Error fetching city populations for {}", countryName, e);
            return Collections.emptyList();
        }
    }
}