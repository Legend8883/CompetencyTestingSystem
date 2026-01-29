package org.legend8883.competencytestingsystem.web;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class PageController {

    @GetMapping
    public String index() {
        return "forward:/index.html";
    }
}
