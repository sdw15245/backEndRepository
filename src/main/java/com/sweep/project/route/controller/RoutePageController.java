package com.sweep.project.route.controller;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class RoutePageController {

    @Value("${api-key.odsay}")
    private String odsayKey;

    @Value("${api-key.korea-data-portal}")
    private String seoulBusApiKey;

    @GetMapping("/")
    public String routePage(Model model) {
        model.addAttribute("odsayKey", odsayKey);
        model.addAttribute("seoulBusApiKey", seoulBusApiKey);
        return "route";
    }
}
