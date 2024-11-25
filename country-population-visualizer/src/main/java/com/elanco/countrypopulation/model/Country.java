package com.elanco.countrypopulation.model;

import lombok.Data;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class Country {
    private String name;
    private String capital;
    private String flag;
    private Long population;


    // Method to format population for display
    public String getFormattedPopulation() {
        if (population == null || population == 0) {
            return "N/A";
        }
        return String.format("%,d", population);
    }

    // Method to get a safe flag URL
    public String getFlagUrl() {
        if (flag == null || flag.trim().isEmpty()) {
            return "https://via.placeholder.com/150x100?text=No+Flag";
        }
        return flag.replace("\\", "").trim();
    }

    // Method to check if capital exists
    public String getFormattedCapital() {
        if (capital == null || capital.trim().isEmpty()) {
            return "N/A";
        }
        return capital;
    }

    // Method to get country name safely
    public String getFormattedName() {
        return name != null ? name : "Unknown Country";
    }
}