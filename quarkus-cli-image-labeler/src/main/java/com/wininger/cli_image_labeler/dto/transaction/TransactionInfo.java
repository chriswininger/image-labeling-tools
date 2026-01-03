package com.wininger.cli_image_labeler.dto.transaction;

import java.time.LocalDate;

import dev.langchain4j.model.output.structured.Description;

public record TransactionInfo(
    @Description("full name") String name,
    @Description("IBAN value") String iban,
    @Description("Date of the transaction") LocalDate transactionDate,
    @Description("Amount in dollars of the transaction") double amount
) {}
