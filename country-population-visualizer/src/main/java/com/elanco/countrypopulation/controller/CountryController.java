package com.elanco.countrypopulation.controller;

import com.elanco.countrypopulation.model.Country;
import com.elanco.countrypopulation.service.CountryService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@ RequestMapping("/api/countries")
public class CountryController {

    private final CountryService countryService;

    @Autowired
    public CountryController(CountryService countryService) {
        this.countryService = countryService;
    }

    @GetMapping
    public ResponseEntity<List<Country>> getAllCountries() {
        return ResponseEntity.ok(countryService.getAllCountries());
    }

    @GetMapping("/{name}")
    public ResponseEntity<Country> getCountryByName(@PathVariable String name) {
        return ResponseEntity.ok(countryService.getCountryByName(name));
    }

    @GetMapping("/{countryName}/cities")
    public ResponseEntity<List<Map<String, Object>>> getCityPopulations(@PathVariable String countryName) {
        return ResponseEntity.ok(countryService.getCityPopulations(countryName));
    }
}