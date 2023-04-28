package uk.gov.dwp.uc.pairtest;

import org.junit.Before;
import org.junit.Test;
import org.mockito.MockitoAnnotations;
import thirdparty.paymentgateway.TicketPaymentService;
import thirdparty.seatbooking.SeatReservationService;
import uk.gov.dwp.uc.pairtest.domain.TicketTypeRequest;
import uk.gov.dwp.uc.pairtest.exception.InvalidPurchaseException;

import static org.mockito.Mockito.*;

public class TicketServiceImplTest {

    private SeatReservationService seatReservationService;
    private TicketPaymentService ticketPaymentService;
    private TicketServiceImpl ticketServiceImpl;

    @Before
    public void setUp() {
        MockitoAnnotations.openMocks(this);
        seatReservationService = mock(SeatReservationService.class);
        ticketPaymentService = mock(TicketPaymentService.class);
        ticketServiceImpl = new TicketServiceImpl(seatReservationService, ticketPaymentService);
    }

    @Test(expected = InvalidPurchaseException.class)
    public void test_invalid_account_id() {
        Long accountId = -1L;
        TicketTypeRequest[] ticketTypeRequests = {
                new TicketTypeRequest(TicketTypeRequest.Type.ADULT, 2),
                new TicketTypeRequest(TicketTypeRequest.Type.ADULT, 2)
        };
        ticketServiceImpl.purchaseTickets(accountId, ticketTypeRequests);
    }

    @Test(expected = InvalidPurchaseException.class)
    public void test_ticket_request_is_zero() {
        Long accountId = 1L;
        TicketTypeRequest[] ticketTypeRequests = {};
        ticketServiceImpl.purchaseTickets(accountId, ticketTypeRequests);
    }

    @Test(expected = InvalidPurchaseException.class)
    public void test_infant_and_child_request_without_adult() {
        Long accountId = 1L;
        TicketTypeRequest[] ticketTypeRequests = {
                new TicketTypeRequest(TicketTypeRequest.Type.INFANT, 2),
                new TicketTypeRequest(TicketTypeRequest.Type.CHILD, 2)
        };
        ticketServiceImpl.purchaseTickets(accountId, ticketTypeRequests);
    }

    @Test(expected = InvalidPurchaseException.class)
    public void test_maximum_ticket_purchase_limit_exceeded() {
        Long accountId = 1L;
        TicketTypeRequest[] ticketTypeRequests = {
                new TicketTypeRequest(TicketTypeRequest.Type.INFANT, 2),
                new TicketTypeRequest(TicketTypeRequest.Type.CHILD, 2),
                new TicketTypeRequest(TicketTypeRequest.Type.ADULT, 19)
        };
        ticketServiceImpl.purchaseTickets(accountId, ticketTypeRequests);
    }

    @Test
    public void test_ticket_purchase_successful() {
        Long accountId = 1L;
        TicketTypeRequest[] ticketTypeRequests = {
                new TicketTypeRequest(TicketTypeRequest.Type.INFANT, 2),
                new TicketTypeRequest(TicketTypeRequest.Type.CHILD, 2),
                new TicketTypeRequest(TicketTypeRequest.Type.ADULT, 18)
        };
        doNothing().when(seatReservationService).reserveSeat(anyLong(), anyInt());
        doNothing().when(ticketPaymentService).makePayment(anyLong(), anyInt());

        ticketServiceImpl.purchaseTickets(accountId, ticketTypeRequests);

        verify(seatReservationService, times(1)).reserveSeat(anyLong(), anyInt());
        verify(ticketPaymentService, times(1)).makePayment(anyLong(), anyInt());
    }
}