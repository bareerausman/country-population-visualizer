package com.elanco.countrypopulation.controller;

import com.elanco.countrypopulation.model.PopulationData;
import com.elanco.countrypopulation.service.PopulationVisualizationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/population-visualization")
public class PopulationVisualizationController {

    private final PopulationVisualizationService populationService;

    @Autowired
    public PopulationVisualizationController(PopulationVisualizationService populationService) {
        this.populationService = populationService;
    }

    @GetMapping("/world-data")
    public ResponseEntity<List<PopulationData>> getWorldPopulationData() {
        return ResponseEntity.ok(populationService.getWorldPopulationData());
    }
}