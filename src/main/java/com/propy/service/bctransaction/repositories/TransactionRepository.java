package com.propy.service.bctransaction.repositories;

import com.propy.service.bctransaction.entities.Transaction;
import org.springframework.data.repository.CrudRepository;

import java.util.List;

public interface TransactionRepository extends CrudRepository<Transaction, String> {

    List<Transaction> findAllByStatus(Transaction.TransactionStatus status);

}
