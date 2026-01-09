package Jaksim.jaksim_server.domain.suggestion.controller;

import Jaksim.jaksim_server.domain.suggestion.service.SuggestionService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import Jaksim.jaksim_server.domain.suggestion.dto.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/suggestion")
public class SuggestionController {

    private final SuggestionService suggestionService;

    @PostMapping("/adjust")
    public SuggestionResponse adjust(@Valid @RequestBody AdjustRequest req) {
        return suggestionService.adjust(req);
    }

    @PostMapping("/new")
    public SuggestionResponse createNew(@Valid @RequestBody NewRequest req) {
        return suggestionService.createNew(req);
    }
}

