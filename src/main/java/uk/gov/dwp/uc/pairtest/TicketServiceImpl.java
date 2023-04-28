package uk.gov.dwp.uc.pairtest;

import thirdparty.paymentgateway.TicketPaymentService;
import thirdparty.seatbooking.SeatReservationService;
import uk.gov.dwp.uc.pairtest.domain.TicketTypeRequest;
import uk.gov.dwp.uc.pairtest.exception.InvalidPurchaseException;

import java.util.stream.Stream;

import static uk.gov.dwp.uc.pairtest.utils.Constants.*;

public class TicketServiceImpl implements TicketService {

    private final SeatReservationService seatReservationService;
    private final TicketPaymentService ticketPaymentService;

    // Constructor injection
    public TicketServiceImpl(SeatReservationService seatReservationService, TicketPaymentService ticketPaymentService) {
        this.seatReservationService = seatReservationService;
        this.ticketPaymentService = ticketPaymentService;
    }

    /**
     * Should only have private methods other than the one below.
     */

    @Override
    public void purchaseTickets(Long accountId, TicketTypeRequest... ticketTypeRequests) throws InvalidPurchaseException {
        if (accountId < 0) {
            throw new InvalidPurchaseException(INVALID_ACCOUNT_ID);
        }

        if (ticketTypeRequests.length == 0) {
            throw new InvalidPurchaseException(INVALID_TICKET_REQUEST);
        }

        validateTicketRequest(ticketTypeRequests);
        validateMaximumTicketPurchase(ticketTypeRequests);
        seatReservationService.reserveSeat(accountId, getNoOfSeatReservation(ticketTypeRequests));
        ticketPaymentService.makePayment(accountId, getAmountToPay(ticketTypeRequests));
    }

    private void validateTicketRequest(TicketTypeRequest[] ticketTypeRequests) {
        int noOfInfantRequests = getNoOfTicketRequestByTicketType(ticketTypeRequests, TicketTypeRequest.Type.INFANT);
        int noOfChildRequests = getNoOfTicketRequestByTicketType(ticketTypeRequests, TicketTypeRequest.Type.CHILD);
        int noOfAdultRequests = getNoOfTicketRequestByTicketType(ticketTypeRequests, TicketTypeRequest.Type.ADULT);
        int childrenRequests = noOfInfantRequests + noOfChildRequests;
        if (childrenRequests > noOfAdultRequests) {
            throw new InvalidPurchaseException(INVALID_INFANT_CHILD_TO_ADULT_REQUEST);
        }
    }

    private int getNoOfTicketRequestByTicketType(TicketTypeRequest[] ticketTypeRequests,
                                                 TicketTypeRequest.Type ticketType) {
        return Stream.of(ticketTypeRequests)
                .filter(ticketRequest -> ticketRequest.getTicketType() == ticketType)
                .mapToInt(TicketTypeRequest::getNoOfTickets)
                .sum();
    }

    private void validateMaximumTicketPurchase(TicketTypeRequest[] ticketTypeRequests) {
        int noOfSeatReservation = getNoOfSeatReservation(ticketTypeRequests);
        if (noOfSeatReservation > MAXIMUM_TICKET_PURCHASE_ALLOWED) {
            throw new InvalidPurchaseException(MAXIMUM_TICKET_PURCHASE_LIMIT_EXCEEDED);
        }
    }

    private int getNoOfSeatReservation(TicketTypeRequest[] ticketTypeRequests) {
        return Stream.of(ticketTypeRequests)
                .filter(ticketRequest -> ticketRequest.getTicketType() != TicketTypeRequest.Type.INFANT)
                .mapToInt(TicketTypeRequest::getNoOfTickets)
                .sum();
    }

    private int getAmountToPay(TicketTypeRequest[] ticketTypeRequests) {
        return Stream.of(ticketTypeRequests)
                .filter(ticketRequest -> ticketRequest.getTicketType() != TicketTypeRequest.Type.INFANT)
                .mapToInt(ticketRequest -> ticketRequest.getNoOfTickets() * getTicketPrice(ticketRequest.getTicketType()))
                .sum();
    }

    private static int getTicketPrice(TicketTypeRequest.Type ticketType) {
        switch (ticketType) {
            case ADULT:
                return 20;
            case CHILD:
                return 10;
            default:
                return 0;
        }
    }
}
