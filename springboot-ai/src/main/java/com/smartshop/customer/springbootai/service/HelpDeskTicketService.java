package com.smartshop.customer.springbootai.service;

import com.smartshop.customer.springbootai.entity.HelpDeskTicket;
import com.smartshop.customer.springbootai.model.TicketRequest;
import com.smartshop.customer.springbootai.repository.HelpDeskTicketRepo;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class HelpDeskTicketService {

    private final HelpDeskTicketRepo helpDeskTicketRepo;

    public HelpDeskTicket createTicket(TicketRequest ticketRequest, String username) {
        HelpDeskTicket helpDeskTicket = HelpDeskTicket.builder()
                .issue(ticketRequest.issue())
                .username(username)
                .status("OPEN")
                .createdAt(LocalDateTime.now())
                .eta(LocalDateTime.now().plusDays(7))
                .build();
        return helpDeskTicketRepo.save(helpDeskTicket);
    }

    public List<HelpDeskTicket> getTicketByUsername(String username) {
        return helpDeskTicketRepo.findByUsername(username);
    }
}
