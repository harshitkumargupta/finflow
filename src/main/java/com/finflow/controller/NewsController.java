package com.finflow.controller;

import com.finflow.dto.NewsResponse;
import com.finflow.service.NewsService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/news")
public class NewsController {
    private final NewsService newsService;

    public NewsController(NewsService newsService) {
        this.newsService = newsService;
    }

    @GetMapping
    public NewsResponse latest() {
        return newsService.latest();
    }

    @GetMapping("/markets")
    public NewsResponse markets() {
        return newsService.markets();
    }

    @GetMapping("/investments")
    public NewsResponse investments() {
        return newsService.investments();
    }

    @GetMapping("/search")
    public NewsResponse search(@RequestParam String q) {
        return newsService.search(q);
    }
}
