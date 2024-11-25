package com.elanco.countrypopulation.controller;

import com.elanco.countrypopulation.service.CountryService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
@RequiredArgsConstructor
public class CountryController {
    private final CountryService countryService;

    @GetMapping("/")
    public String showCountries(Model model) {
        model.addAttribute("countries", countryService.getAllCountries());
        return "index";
    }
}
