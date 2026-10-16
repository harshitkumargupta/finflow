package com.finflow.service;

import org.springframework.data.domain.Pageable;
import com.finflow.dto.PageResponseMapper;
import com.finflow.dto.PageResponse;
import com.finflow.dto.CardRequest;
import com.finflow.dto.CardResponse;
import com.finflow.entity.Card;
import com.finflow.entity.User;
import com.finflow.exception.ResourceNotFoundException;
import com.finflow.repository.AutopayAuthorizationRepository;
import com.finflow.repository.CardRepository;
import com.finflow.repository.FinanceNotificationRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class CardService {

    private final CurrentUserService currentUserService;
    private final CardRepository cardRepository;
    private final AutopayAuthorizationRepository autopayRepository;
    private final FinanceNotificationRepository notificationRepository;

    public CardService(CurrentUserService currentUserService,
                       CardRepository cardRepository,
                       AutopayAuthorizationRepository autopayRepository,
                       FinanceNotificationRepository notificationRepository) {
        this.currentUserService = currentUserService;
        this.cardRepository = cardRepository;
        this.autopayRepository = autopayRepository;
        this.notificationRepository = notificationRepository;
    }

    @Transactional
    public CardResponse addCard(CardRequest request) {
        User user = currentUserService.get();
        Card card = new Card();
        card.setUser(user);
        card.setCardholderName(request.cardholderName().trim());

        String last4 = request.lastFourDigits().trim();
        card.setLastFourDigits(last4);
        card.setCardNetwork(request.cardNetwork().trim().toUpperCase());
        card.setExpiryMonth(request.expiryMonth());
        card.setExpiryYear(request.expiryYear());

        card.setTokenReference(null);
        
        String network = card.getCardNetwork();
        String nickname = request.nickname() != null && !request.nickname().isBlank()
                ? request.nickname().trim()
                : network + " ending in " + last4;
        card.setNickname(nickname);
        
        card.setCardType("SANDBOX_METADATA");
        
        card.setStatus("ACTIVE");
        card.setCreatedAt(LocalDateTime.now());

        Card saved = cardRepository.save(card);
        
        // Notify user about newly added payment source
        notificationRepository.createIfAbsent(
                user.getId(),
                "CARD_ADDED:" + saved.getId(),
                "CARD_ADDED",
                "Payment source added",
                "Your sandbox card metadata '" + nickname + "' was added to your profile.",
                "INFO"
        );

        return toResponse(saved, 0);
    }

    @Transactional(readOnly = true)
    public List<CardResponse> listCards() {
        User user = currentUserService.get();
        List<com.finflow.entity.AutopayAuthorization> authorizations = autopayRepository.findByUserIdOrderByNextPaymentDateAsc(user.getId());
        Map<Long, Long> autopayCounts = authorizations.stream()
                .filter(a -> a.getCard() != null && !"CANCELLED".equalsIgnoreCase(a.getStatus()))
                .collect(Collectors.groupingBy(a -> a.getCard().getId(), Collectors.counting()));
        return cardRepository.findByUserIdAndStatusInOrderByCreatedAtDesc(user.getId(), List.of("ACTIVE", "DISABLED"))
                .stream().map(card -> toResponse(card, autopayCounts.getOrDefault(card.getId(), 0L))).toList();
    }

    @Transactional(readOnly = true)
    public PageResponse<CardResponse> listCards(Pageable pageable) {
        User user = currentUserService.get();
        Map<Long, Long> autopayCounts = autopayRepository.findByUserIdOrderByNextPaymentDateAsc(user.getId()).stream()
                .filter(a -> a.getCard() != null && !"CANCELLED".equalsIgnoreCase(a.getStatus()))
                .collect(Collectors.groupingBy(a -> a.getCard().getId(), Collectors.counting()));
        return PageResponseMapper.map(
                cardRepository.findByUserIdAndStatusInOrderByCreatedAtDescIdDesc(user.getId(), List.of("ACTIVE", "DISABLED"), pageable),
                card -> toResponse(card, autopayCounts.getOrDefault(card.getId(), 0L)));
    }

    @Transactional(readOnly = true)
    public CardResponse getCard(Long id) {
        User user = currentUserService.get();
        Card card = cardRepository.findByIdAndUserId(id, user.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Card not found"));
        return toResponse(card, autopayRepository.findByUserIdOrderByNextPaymentDateAsc(user.getId()).stream()
                .filter(a -> a.getCard() != null && a.getCard().getId().equals(card.getId()) && !"CANCELLED".equalsIgnoreCase(a.getStatus()))
                .count());
    }

    @Transactional
    public CardResponse toggleCardStatus(Long id) {
        User user = currentUserService.get();
        Card card = cardRepository.findByIdAndUserId(id, user.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Card not found"));
        
        if ("REMOVED".equalsIgnoreCase(card.getStatus())) {
            throw new ResourceNotFoundException("Card not found");
        }

        String newStatus = "ACTIVE".equalsIgnoreCase(card.getStatus()) ? "DISABLED" : "ACTIVE";
        card.setStatus(newStatus);
        Card updated = cardRepository.save(card);

        if ("DISABLED".equals(newStatus)) {
            notificationRepository.createIfAbsent(
                    user.getId(),
                    "CARD_DISABLED:" + card.getId() + ":" + System.currentTimeMillis(),
                    "CARD_DISABLED",
                    "Payment source disabled",
                    "Payment source '" + (card.getNickname() != null ? card.getNickname() : "•••• " + card.getLastFourDigits()) + "' has been disabled.",
                    "WARNING"
            );
        }

        return toResponse(updated, 0);
    }

    @Transactional
    public void deleteCard(Long id) {
        User user = currentUserService.get();
        Card card = cardRepository.findByIdAndUserId(id, user.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Card not found"));
        
        card.setStatus("REMOVED");
        cardRepository.save(card);

        notificationRepository.createIfAbsent(
                user.getId(),
                "CARD_REMOVED:" + card.getId() + ":" + System.currentTimeMillis(),
                "CARD_REMOVED",
                "Payment source removed",
                "Payment source '" + (card.getNickname() != null ? card.getNickname() : "•••• " + card.getLastFourDigits()) + "' was removed.",
                "INFO"
        );
    }

    private CardResponse toResponse(Card card, long autopayCount) {
        return new CardResponse(
                card.getId(),
                card.getNickname(),
                card.getCardType() != null ? card.getCardType() : "DEBIT",
                card.getCardholderName(),
                card.getLastFourDigits(),
                card.getCardNetwork(),
                card.getExpiryMonth(),
                card.getExpiryYear(),
                card.getStatus(),
                autopayCount,
                card.getCreatedAt()
        );
    }
}
