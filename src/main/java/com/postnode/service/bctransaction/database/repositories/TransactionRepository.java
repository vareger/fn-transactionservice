package com.postnode.service.bctransaction.database.repositories;

import com.postnode.service.bctransaction.database.entities.Transaction;
import org.springframework.data.repository.CrudRepository;

import java.util.List;

public interface TransactionRepository extends CrudRepository<Transaction, String> {

    List<Transaction> findAllByStatus(Transaction.TransactionStatus status);

}
