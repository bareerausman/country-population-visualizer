package com.elanco.countrypopulation.service;

import com.elanco.countrypopulation.model.Country;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.server.ResponseStatusException;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

@Service
public class CountryService {

    // Primary APIs
    private static final String COUNTRIES_NOW_BASE = "https://countriesnow.space/api/v0.1";
    private static final String REST_COUNTRIES_API = "https://restcountries.com/v3.1";
    private static final String POPULATION_API = "https://population.un.org/dataportal-api/v1";

    private final RestTemplate restTemplate;

    @Autowired
    public CountryService(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    /**
     * Get all countries with complete information
     */
    @Cacheable("countries")
    public List<Country> getAllCountries() {
        try {
            // Get base country data from REST Countries API
            List<Country> countries = getBaseCountryData();

            // Enrich with additional data
            enrichWithDetailedData(countries);

            return countries.stream()
                    .filter(c -> c.getName() != null && c.getPopulation() != null)
                    .sorted(Comparator.comparing(Country::getName))
                    .collect(Collectors.toList());

        } catch (Exception e) {
            throw new ResponseStatusException(
                    HttpStatus.INTERNAL_SERVER_ERROR,
                    "Failed to retrieve country data: " + e.getMessage()
            );
        }
    }

    /**
     * Get base country data from REST Countries API
     */
    private List<Country> getBaseCountryData() {
        String url = REST_COUNTRIES_API + "/all?fields=name,capital,population,flags,area";
        ResponseEntity<Map[]> response = restTemplate.getForEntity(url, Map[].class);

        return Arrays.stream(response.getBody())
                .map(this::mapToCountry)
                .collect(Collectors.toList());
    }

    /**
     * Map REST Countries API response to Country object
     */
    @SuppressWarnings("unchecked")
    private Country mapToCountry(Map<String, Object> data) {
        Country country = new Country();

        // Get common name
        Map<String, Object> name = (Map<String, Object>) data.get("name");
        country.setName((String) name.get("common"));

        // Get capital (first one if multiple)
        List<String> capitals = (List<String>) data.get("capital");
        if (capitals != null && !capitals.isEmpty()) {
            country.setCapital(capitals.get(0));
        }

        // Get population
        country.setPopulation(((Number) data.get("population")).longValue());

        // Get flag URLs
        Map<String, String> flags = (Map<String, String>) data.get("flags");
        if (flags != null) {
            // Prefer SVG, fallback to PNG
            country.setFlag(flags.getOrDefault("svg", flags.get("png")));
        }

        return country;
    }

    /**
     * Enrich countries with additional data from multiple sources
     */
    private void enrichWithDetailedData(List<Country> countries) {
        // Create parallel tasks for enrichment
        CompletableFuture<Void> populationFuture = CompletableFuture
                .runAsync(() -> enrichWithUNPopulation(countries));

        CompletableFuture<Void> additionalDataFuture = CompletableFuture
                .runAsync(() -> enrichWithAdditionalData(countries));

        // Wait for all enrichment tasks to complete
        CompletableFuture.allOf(populationFuture, additionalDataFuture).join();
    }

    /**
     * Enrich with UN population data
     */
    private void enrichWithUNPopulation(List<Country> countries) {
        try {
            String url = POPULATION_API + "/data/indicators/49/locations";
            ResponseEntity<Map> response = restTemplate.getForEntity(url, Map.class);

            if (response.getBody() != null && response.getBody().containsKey("data")) {
                List<Map<String, Object>> populationData = (List<Map<String, Object>>) response.getBody().get("data");

                Map<String, Long> populationMap = new HashMap<>();
                for (Map<String, Object> data : populationData) {
                    String countryName = (String) data.get("location");
                    Number value = (Number) data.get("value");
                    if (value != null) {
                        populationMap.put(countryName, value.longValue());
                    }
                }

                // Update population data if available
                countries.forEach(country -> {
                    Long population = populationMap.get(country.getName());
                    if (population != null && population > 0) {
                        country.setPopulation(population);
                    }
                });
            }
        } catch (Exception e) {
            System.err.println("Failed to get UN population data: " + e.getMessage());
        }
    }

    /**
     * Enrich with additional country data
     */
    private void enrichWithAdditionalData(List<Country> countries) {
        try {
            String url = COUNTRIES_NOW_BASE + "/countries/info?returns=currency,iso2,iso3,languages";
            ResponseEntity<Map> response = restTemplate.getForEntity(url, Map.class);

            if (response.getBody() != null && response.getBody().containsKey("data")) {
                List<Map<String, Object>> additionalData = (List<Map<String, Object>>) response.getBody().get("data");

                Map<String, Map<String, Object>> dataMap = additionalData.stream()
                        .collect(Collectors.toMap(
                                data -> (String) data.get("name"),
                                data -> data,
                                (existing, replacement) -> existing
                        ));

                countries.forEach(country -> {
                    Map<String, Object> data = dataMap.get(country.getName());
                    if (data != null) {
                        // Enrich with any additional data you want to include
                        // For example: currency, ISO codes, languages, etc.
                    }
                });
            }
        } catch (Exception e) {
            System.err.println("Failed to get additional country data: " + e.getMessage());
        }
    }

    /**
     * Get a specific country by name
     */
    @Cacheable("country")
    public Country getCountryByName(String name) {
        try {
            String url = REST_COUNTRIES_API + "/name/" + name + "?fullText=true";
            ResponseEntity<Map[]> response = restTemplate.getForEntity(url, Map[].class);

            if (response.getBody() != null && response.getBody().length > 0) {
                Country country = mapToCountry(response.getBody()[0]);

                // Enrich with additional data
                List<Country> countries = new ArrayList<>();
                countries.add(country);
                enrichWithDetailedData(countries);

                return country;
            }

            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Country not found: " + name);
        } catch (Exception e) {
            throw new ResponseStatusException(
                    HttpStatus.INTERNAL_SERVER_ERROR,
                    "Failed to retrieve country data: " + e.getMessage()
            );
        }
    }

    /**
     * Search countries by name
     */
    public List<Country> searchCountries(String query) {
        return getAllCountries().stream()
                .filter(country -> country.getName().toLowerCase().contains(query.toLowerCase()))
                .collect(Collectors.toList());
    }
}