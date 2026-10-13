package com.msspoker.rankingservice.controller;

import com.msspoker.rankingservice.dto.response.SeasonResponse;
import com.msspoker.rankingservice.service.SeasonService;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/seasons")
public class SeasonController {
    private final SeasonService seasons;

    @GetMapping("/current")
    public SeasonResponse current() {
        return seasons.current();
    }
}
