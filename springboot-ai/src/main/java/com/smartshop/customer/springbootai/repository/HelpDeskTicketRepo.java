package com.smartshop.customer.springbootai.repository;

import com.smartshop.customer.springbootai.entity.HelpDeskTicket;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface HelpDeskTicketRepo extends JpaRepository<HelpDeskTicket,Long> {

    List<HelpDeskTicket> findByUsername(String username);
}
