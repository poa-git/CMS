package com.system.complaints.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.PathVariable;

@Controller
public class FallbackController {

    @RequestMapping(value = "/{path:[^\\.]*}")
    public String redirect() {
        // Forward to `index.html` to allow React to handle routing
        return "forward:/index.html";
    }
}
