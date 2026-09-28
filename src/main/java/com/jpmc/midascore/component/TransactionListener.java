package com.jpmc.midascore.component;

import com.jpmc.midascore.foundation.Transaction;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
public class TransactionListener {
    private static final Logger logger = LoggerFactory.getLogger(TransactionListener.class);
    
    private final DatabaseConduit databaseConduit;
    private final IncentiveApi incentiveApi;

    public TransactionListener(DatabaseConduit databaseConduit, IncentiveApi incentiveApi) {
        this.databaseConduit = databaseConduit;
        this.incentiveApi = incentiveApi;
    }

    @KafkaListener(topics = "${general.kafka-topic}")
    public void listen(Transaction transaction) {
        logger.info("Received transaction: {}", transaction);
        
        com.jpmc.midascore.entity.UserRecord sender = databaseConduit.findById(transaction.getSenderId());
        com.jpmc.midascore.entity.UserRecord recipient = databaseConduit.findById(transaction.getRecipientId());

        if (sender != null && recipient != null) {
            if (sender.getBalance() >= transaction.getAmount()) {
                com.jpmc.midascore.foundation.Incentive incentive = incentiveApi.getIncentive(transaction);
                float totalAmount = transaction.getAmount() + incentive.getAmount();

                sender.setBalance(sender.getBalance() - transaction.getAmount());
                recipient.setBalance(recipient.getBalance() + totalAmount);
                
                databaseConduit.save(sender);
                databaseConduit.save(recipient);
                logger.info("Transaction processed successfully with incentive: {}", incentive.getAmount());
            } else {
                logger.info("Transaction failed: insufficient funds.");
            }
        } else {
            logger.info("Transaction failed: invalid sender or recipient.");
        }
    }
}
