package com.finwise.repository;
import com.finwise.model.User;
import com.finwise.model.Transaction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface TransactionRepository extends JpaRepository<Transaction, Long> {
    //List<Transaction> findByUser10OrderByDateDesc(User user);


    List<Transaction> findByUserAndType(User user, String type);


    List<Transaction> findTop10ByUserOrderByDateDesc(User user);

    List<Transaction> findByUserOrderByDateDesc(User user);
}
