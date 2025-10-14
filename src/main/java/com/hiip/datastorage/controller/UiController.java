package com.hiip.datastorage.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

/**
 * Controller for serving the web UI and handling UI-related routes.
 */
@Controller
public class UiController {

    /**
     * Redirect root path to UI
     */
    @GetMapping("/")
    public String redirectToUI() {
        return "redirect:/ui/index.html";
    }

    /**
     * Serve the main UI page
     */
    @GetMapping("/ui")
    public String uiRedirect() {
        return "redirect:/ui/index.html";
    }
}
