package com.elanco.countrypopulation.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class PopulationData {
    private String countryCode;
    private String countryName;
    private Long totalPopulation;
    private Double populationDensity;
    private String colorCode;
}