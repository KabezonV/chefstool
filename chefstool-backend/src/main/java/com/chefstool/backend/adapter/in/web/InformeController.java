package com.chefstool.backend.adapter.in.web;

import com.chefstool.backend.application.InformeService;
import com.chefstool.backend.application.dto.PanelInforme;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/informes")
public class InformeController {

    private final InformeService informeService;

    public InformeController(InformeService informeService) {
        this.informeService = informeService;
    }

    @GetMapping("/panel")
    public PanelInforme panel() {
        return informeService.panel();
    }
}
