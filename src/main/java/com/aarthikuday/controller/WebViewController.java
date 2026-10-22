package com.aarthikuday.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class WebViewController {

    @GetMapping({"/portal", "/dashboard", "/admin"})
    public String forwardToPortal() {
        return "forward:/portal.html";
    }
}
