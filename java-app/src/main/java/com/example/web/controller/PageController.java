package com.example.web.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class PageController {

    @GetMapping("/")
    public String home() {
        return "Hello, this is HomE... page, evening date 14 jan 2026";
    }

    
    @GetMapping("/about")
    public String about() {
        return "This is ABOUT page";
    }



    @GetMapping("/contact")
    public String contact() {
    return "This is the contact page and you can connect at 8588037474";
    }
   
}
