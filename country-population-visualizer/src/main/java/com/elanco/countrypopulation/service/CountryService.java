package com.elanco.countrypopulation.service;

import com.elanco.countrypopulation.model.Country;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.*;

@Service
@RequiredArgsConstructor
public class CountryService {
    private final RestTemplate restTemplate;
    private static final String BASE_URL = "https://countriesnow.space/api/v0.1";

    @Cacheable("countries")
    public List<Country> getAllCountries() {
        List<Country> countries = getCountriesWithCapitals();
        enrichWithFlags(countries);
        enrichWithPopulation(countries);
        return countries;
    }

    private List<Country> getCountriesWithCapitals() {
        ResponseEntity<Map> response = restTemplate.getForEntity(
                BASE_URL + "/countries/capital",
                Map.class
        );

        List<LinkedHashMap<String, String>> data = (List<LinkedHashMap<String, String>>)
                response.getBody().get("data");

        List<Country> countries = new ArrayList<>();

        for (LinkedHashMap<String, String> countryData : data) {
            Country country = new Country();
            country.setName(countryData.get("name"));
            country.setCapital(countryData.get("capital"));
            countries.add(country);
        }

        return countries;
    }

    private void enrichWithFlags(List<Country> countries) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        for (Country country : countries) {
            Map<String, String> requestBody = new HashMap<>();
            requestBody.put("country", country.getName());

            HttpEntity<Map<String, String>> request = new HttpEntity<>(requestBody, headers);

            try {
                ResponseEntity<Map> response = restTemplate.postForEntity(
                        BASE_URL + "/countries/flag/images",
                        request,
                        Map.class
                );

                if (response.getBody() != null && response.getBody().get("data") != null) {
                    Map<String, Object> flagData = (Map<String, Object>) response.getBody().get("data");

                    String flagUrl = null;
                    if (flagData.containsKey("flag")) {
                        flagUrl = String.valueOf(flagData.get("flag"));
                    } else if (flagData.containsKey("png")) {
                        flagUrl = String.valueOf(flagData.get("png"));
                    }

                    if (flagUrl != null) {
                        flagUrl = flagUrl.replace("\\", "").trim();
                        if (flagUrl.startsWith("//")) {
                            flagUrl = "https:" + flagUrl;
                        }
                        country.setFlag(flagUrl);
                    }
                }
            } catch (Exception e) {
                System.out.println("Error fetching flag for " + country.getName() + ": " + e.getMessage());
            }
        }
    }

    private void enrichWithPopulation(List<Country> countries) {
        try {
            ResponseEntity<Map> response = restTemplate.getForEntity(
                    BASE_URL + "/countries/population/cities",
                    Map.class
            );

            if (response.getBody() != null) {
                List<LinkedHashMap<String, Object>> data = (List<LinkedHashMap<String, Object>>)
                        response.getBody().get("data");

                Map<String, Long> populationByCountry = new HashMap<>();

                for (LinkedHashMap<String, Object> cityData : data) {
                    String countryName = (String) cityData.get("country");
                    List<LinkedHashMap<String, Object>> populationCounts =
                            (List<LinkedHashMap<String, Object>>) cityData.get("populationCounts");

                    if (!populationCounts.isEmpty()) {
                        LinkedHashMap<String, Object> latestCount = populationCounts.get(populationCounts.size() - 1);

                        Object populationObj = latestCount.get("value");
                        Long population = 0L;

                        if (populationObj instanceof String) {
                            population = Long.parseLong(((String) populationObj).replace(",", ""));
                        } else if (populationObj instanceof Number) {
                            population = ((Number) populationObj).longValue();
                        }

                        populationByCountry.merge(countryName, population, Long::sum);
                    }
                }

                for (Country country : countries) {
                    country.setPopulation(populationByCountry.getOrDefault(country.getName(), 0L));
                }
            }
        } catch (Exception e) {
            System.out.println("Error fetching population data: " + e.getMessage());
        }
    }
}