package com.finflow.controller;

import com.finflow.dto.CardRequest;
import com.finflow.dto.CardResponse;
import com.finflow.service.CardService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/cards")
public class CardController {

    private final CardService cardService;

    public CardController(CardService cardService) {
        this.cardService = cardService;
    }

    @PostMapping
    public ResponseEntity<CardResponse> addCard(@Valid @RequestBody CardRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(cardService.addCard(request));
    }

    @GetMapping
    public Object listCards(@RequestParam(required = false) Integer page, @RequestParam(required = false) Integer size) {
        return Paging.requested(page, size) ? cardService.listCards(Paging.of(page, size)) : cardService.listCards();
    }

    @GetMapping("/{id}")
    public CardResponse getCard(@PathVariable Long id) {
        return cardService.getCard(id);
    }

    @PutMapping("/{id}/status")
    public CardResponse toggleCardStatus(@PathVariable Long id) {
        return cardService.toggleCardStatus(id);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteCard(@PathVariable Long id) {
        cardService.deleteCard(id);
        return ResponseEntity.noContent().build();
    }
}
