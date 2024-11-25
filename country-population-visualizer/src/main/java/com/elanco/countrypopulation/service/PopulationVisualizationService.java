package com.elanco.countrypopulation.service;

import com.elanco.countrypopulation.model.Country;
import com.elanco.countrypopulation.model.PopulationData;
import com.elanco.countrypopulation.util.ColorMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
public class PopulationVisualizationService {

    private static final String POPULATION_API = "https://countriesnow.space/api/v0.1/countries/population";
    private final RestTemplate restTemplate;
    private final CountryService countryService;

    @Autowired
    public PopulationVisualizationService(RestTemplate restTemplate, CountryService countryService) {
        this.restTemplate = restTemplate;
        this.countryService = countryService;
    }

    @Cacheable("populationData")
    public List<PopulationData> getWorldPopulationData() {
        List<Country> countries = countryService.getAllCountries();

        return countries.stream()
                .map(this::enrichPopulationData)
                .collect(Collectors.toList());
    }

    private PopulationData enrichPopulationData(Country country) {
        try {
            // Fetch population data
            Long population = fetchPopulationForCountry(country.getName());

            // Calculate population density (mock calculation)
            double density = calculatePopulationDensity(population);

            return new PopulationData(
                    getCountryCode(country.getName()),
                    country.getName(),
                    population,
                    density,
                    ColorMapper.getColorForPopulationDensity(density)
            );
        } catch (Exception e) {
            log.error("Error enriching population data for {}", country.getName(), e);
            return new PopulationData(
                    getCountryCode(country.getName()),
                    country.getName(),
                    0L,
                    0.0,
                    "#CCCCCC"  // Default grey for no data
            );
        }
    }

    private Long fetchPopulationForCountry(String countryName) {
        try {
            // Implement actual API call to get population
            // This is a placeholder - replace with actual implementation
            return 1000000L;  // Mock population
        } catch (Exception e) {
            log.error("Failed to fetch population for {}", countryName);
            return 0L;
        }
    }

    private double calculatePopulationDensity(Long population) {
        // Mock density calculation
        return Math.log(population + 1);
    }

    private String getCountryCode(String countryName) {
        // Implement country code mapping
        // You might want to use a comprehensive country code mapping
        return countryName.substring(0, 2).toUpperCase();
    }
}