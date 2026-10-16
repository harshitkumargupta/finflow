package com.finflow.dto;

import jakarta.validation.constraints.NotNull;

public record NotificationStateRequest(@NotNull Boolean read) { }
