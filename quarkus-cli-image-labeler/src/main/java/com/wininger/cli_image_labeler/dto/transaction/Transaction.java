package com.wininger.cli_image_labeler.dto.transaction;

import dev.langchain4j.service.UserMessage;

public interface Transaction {
    @UserMessage("Extract information about a transaction from {{it}}")
    TransactionInfo extract(String message);
}
