package com.gpstracker.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

/**
 * Controller for web UI
 */
@Controller
public class WebController {

    /**
     * Dashboard page - use the Cyber Terminal UI
     */
    @GetMapping({"/", "/dashboard", "/terminal"})
    public String dashboard() {
        return "cyber-terminal";
    }
    
    @GetMapping("/test")
    public String test() {
        return "test-page";
    }
}
