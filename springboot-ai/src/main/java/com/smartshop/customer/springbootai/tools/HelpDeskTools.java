package com.smartshop.customer.springbootai.tools;

import com.smartshop.customer.springbootai.entity.HelpDeskTicket;
import com.smartshop.customer.springbootai.model.TicketRequest;
import com.smartshop.customer.springbootai.service.HelpDeskTicketService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.model.ToolContext;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class HelpDeskTools {

    private static final Logger LOGGER = LoggerFactory.getLogger(HelpDeskTools.class.getName());

    private final HelpDeskTicketService helpDeskTicketService;

    /**
     * Creates a support ticket.
     *
     * <p>The LLM provides the {@link TicketRequest} input based on the user's
     * request. {@link ToolContext} is provided by Spring AI and is used to
     * get the current username.</p>
     */
    @Tool(name = "createTicket", description = "Create the support ticket")
    String createTicket(
            @ToolParam(description = "Details to create a support ticket")
            TicketRequest ticketRequest,
            ToolContext toolContext) {

        String username = (String) toolContext.getContext().get("username");
        LOGGER.info("Creating support ticket for user: {} with details: {}", username, ticketRequest);
        HelpDeskTicket savedHelpDeskTicket =
                helpDeskTicketService.createTicket(ticketRequest, username);
        LOGGER.info("Ticket created successfully. Ticket ID: {}, Username: {}", savedHelpDeskTicket.getId(), username);

        return "Ticket #" + savedHelpDeskTicket.getId()
                + " created successfully for user "
                + savedHelpDeskTicket.getUsername();
    }

    /**
     * Fetches the current user's open ticket status.
     *
     * <p>This method does not require input from the LLM. The username is
     * obtained from the {@link ToolContext}, which is provided by Spring AI.</p>
     *
     * <p>The tool name is automatically taken from the method name
     * {@code getTicketStatus} because the {@code name} attribute is not specified.</p>
     */
    @Tool(description = "Fetch the status of the open tickets based on a given username")
    List<HelpDeskTicket> getTicketStatus(ToolContext toolContext) {

        String username = (String) toolContext.getContext().get("username");
        LOGGER.info("Fetching tickets for user: {}", username);
        List<HelpDeskTicket> ticketsList = helpDeskTicketService.getTicketByUsername(username);
        LOGGER.info("Found {} tickets for user: {}", ticketsList.size(), username);
        return ticketsList;
    }
}