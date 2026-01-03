package com.wininger.cli_image_labeler;

import com.wininger.cli_image_labeler.dto.transaction.Transaction;
import com.wininger.cli_image_labeler.dto.transaction.TransactionInfo;

import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.ollama.OllamaChatModel;
import dev.langchain4j.service.AiServices;
import picocli.CommandLine.Command;

@Command(name = "transaction", mixinStandardHelpOptions = true)
public class TransactionCommand implements Runnable {
   @Override
   public void run() {
    final ChatModel chatModel = OllamaChatModel.builder()
        .modelName("gemma3:4b")
        .baseUrl("http://localhost:11434/")
        .build();

    final Transaction tx = AiServices.builder(Transaction.class)
        .chatModel(chatModel)
        .build();

    final TransactionInfo transactionInfo = tx.extract(
        """
        My name is Alex; I did a transaction on July 4th, 2023 from my acount
        with IBAN 123456789 of $25.50";
                """);

    System.out.println(transactionInfo);
   }
}
