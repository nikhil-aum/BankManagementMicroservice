    package com.bankManagement.account_service.service.impl;


    import com.bankManagement.account_service.dto.CustomerExistsResponseDTO;
    import com.bankManagement.account_service.dto.MoneyTransferRequestDTO;
    import com.bankManagement.account_service.dto.TransactionResponseDTO;
    import com.bankManagement.account_service.entity.Account;
    import com.bankManagement.account_service.entity.Transaction;
    import com.bankManagement.account_service.entity.TransactionStatus;
    import com.bankManagement.account_service.entity.TransactionType;
    import com.bankManagement.account_service.exception.AccountOwnershipException;
    import com.bankManagement.account_service.exception.BankingException;
    import com.bankManagement.account_service.feign.CustomerClient;
    import com.bankManagement.account_service.repository.AccountRepository;
    import com.bankManagement.account_service.service.TransferService;
    import lombok.AllArgsConstructor;
    import org.slf4j.Logger;
    import org.slf4j.LoggerFactory;
    import org.springframework.stereotype.Service;

    import java.math.BigDecimal;

    @Service
    @AllArgsConstructor
    public class TransferServiceImpl implements TransferService {
        private static final Logger logger = LoggerFactory.getLogger(TransferServiceImpl.class);
        private final AccountRepository accountRepository;
        private final CustomerClient customerClient;


        @Override
        public TransactionResponseDTO transfer(MoneyTransferRequestDTO request, Long customerId) {
            logger.info("Transfer request: Sender={}, Recipient={}, Amount={}",
                    request.getSenderAccountNumber(), request.getRecipientAccountNumber(), request.getAmount());

            if (!request.getRecipientAccountNumber().equals(request.getConfirmRecipientAccountNumber())) {
                throw new BankingException("Something went wrong.....");
            }

            Account sender = accountRepository.findById(request.getSenderAccountNumber())
                    .orElseThrow(() -> {
                        logger.error("Sender account not found: {}", request.getSenderAccountNumber());
                        return new AccountOwnershipException();
                    });

            if (!sender.getCustomerId().equals(customerId)) {
                logger.warn("Ownership mismatch for account {} and customer {}", request.getSenderAccountNumber(), customerId);
                throw new AccountOwnershipException();
            }


            Account recipient = accountRepository.findById(request.getRecipientAccountNumber())
                    .orElseThrow(() -> {
                        logger.error("Recipient account not found: {}", request.getRecipientAccountNumber());
                        return new BankingException("Recipient account not exist..... ");
                    });

            if (sender.getAccountNumber().equals(recipient.getAccountNumber())) {
                logger.warn("Transfer failed: Sender and recipient accounts are same ({})", sender.getAccountNumber());
                throw new BankingException("Transfer failed: Sender and recipient accounts can't be same");
            }

            if (request.getAmount() == null || request.getAmount().compareTo(BigDecimal.ZERO) <= 0) {
                logger.warn("Transfer failed: Invalid amount {} from sender {}", request.getAmount(), sender.getAccountNumber());
                Transaction failedTx = new Transaction(
                        null,
                        TransactionType.TRANSFER_OUT,
                        request.getAmount(),
                        null,
                        "Transfer failed: Amount must be greater than 0",
                        sender.getBalance(),
                        TransactionStatus.FAILED,
                        sender
                );
                sender.getTransactions().add(failedTx);
                accountRepository.save(sender);
                throw new BankingException("Amount must be greater than 0");
            }

            if (request.getAmount().compareTo(sender.getBalance()) > 0) {
                logger.warn("Transfer failed: Insufficient balance. Sender={}, Balance={}, Requested={}",
                        sender.getAccountNumber(), sender.getBalance(), request.getAmount());
                Transaction failedTx = new Transaction(
                        null,
                        TransactionType.TRANSFER_OUT,
                        request.getAmount(),
                        null,
                        "Transfer failed: Insufficient balance",
                        sender.getBalance(),
                        TransactionStatus.FAILED,
                        sender
                );
                sender.getTransactions().add(failedTx);
                accountRepository.save(sender);
                throw new BankingException("Insufficient balance");
            }

            sender.withdraw(request.getAmount());
            recipient.deposit(request.getAmount());

            Transaction senderTx = new Transaction(
                    null,
                    TransactionType.TRANSFER_OUT,
                    request.getAmount(),
                    null,
                    "₹" + request.getAmount() + " transferred to account " + recipient.getAccountNumber(),
                    sender.getBalance(),
                    TransactionStatus.SUCCESS,
                    sender
            );

            Transaction recipientTx = new Transaction(
                    null,
                    TransactionType.TRANSFER_IN,
                    request.getAmount(),
                    null,
                    "₹" + request.getAmount() + " received from account " + sender.getAccountNumber(),
                    recipient.getBalance(),
                    TransactionStatus.SUCCESS,
                    recipient
            );

            sender.getTransactions().add(senderTx);
            recipient.getTransactions().add(recipientTx);

            accountRepository.save(sender);
            accountRepository.save(recipient);


            logger.info("Transfer successful: ₹{} from {} to {}. Sender balance={}, Recipient balance={}",
                    request.getAmount(), sender.getAccountNumber(), recipient.getAccountNumber(),
                    sender.getBalance(), recipient.getBalance());

            CustomerExistsResponseDTO senderCustomer = customerClient.customerExists(sender.getCustomerId());
            CustomerExistsResponseDTO recipientCustomer = customerClient.customerExists(recipient.getCustomerId());

            TransactionResponseDTO response = new TransactionResponseDTO();
            response.setMessage("₹" + request.getAmount()
                    + " transferred successfully from " + senderCustomer.getCustomerName()
                    + " to " + recipientCustomer.getCustomerName());
            return response;
        }
    }

