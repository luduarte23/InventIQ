package com.inventiq.web.pos;

import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;

import com.inventiq.application.pos.SalesService;
import com.inventiq.application.security.AuthenticatedUser;
import com.inventiq.domain.model.Role;
import com.inventiq.domain.model.Sale;
import com.inventiq.domain.model.exception.DomainException;
import com.inventiq.support.TestUsers;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

/**
 * La venta se abre a nombre del usuario con sesión iniciada, no de un id fijo (A7).
 * SalesService se reemplaza por un doble: aquí solo interesa con qué id lo llama el controlador.
 */
@SpringBootTest
@AutoConfigureMockMvc
class PosControllerCashierTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private SalesService salesService;

    @BeforeEach
    void setUp() {
        // startSale devuelve una venta "abierta" con id 55.
        Sale started = mock(Sale.class);
        when(started.getId()).thenReturn(55L);
        when(salesService.startSale(anyLong())).thenReturn(started);
        // El escaneo se rechaza (como con un código desconocido): el controlador muestra el error
        // y sigue. Así la prueba no depende de productos ni de inventario.
        when(salesService.scan(anyLong(), anyString(), anyInt())).thenThrow(mock(DomainException.class));
    }

    @Test
    void theSaleIsOpenedForTheLoggedInCashier() throws Exception {
        scanAs(TestUsers.cashier(), new MockHttpSession());

        verify(salesService).startSale(TestUsers.CASHIER.userId());
    }

    @Test
    void anotherUserGetsTheSaleUnderTheirOwnId() throws Exception {
        AuthenticatedUser other = new AuthenticatedUser(7L, "otra-cajera", Role.CASHIER);

        scanAs(TestUsers.as(other), new MockHttpSession());

        verify(salesService).startSale(7L);
    }

    @Test
    void aSecondScanInTheSameSessionKeepsTheSameSale() throws Exception {
        MockHttpSession session = new MockHttpSession();

        scanAs(TestUsers.cashier(), session);
        scanAs(TestUsers.cashier(), session);

        verify(salesService, times(1)).startSale(TestUsers.CASHIER.userId());
        verify(salesService, times(2)).scan(55L, "7700000000001", 1);
    }

    private void scanAs(RequestPostProcessor user, MockHttpSession session) throws Exception {
        mockMvc.perform(post("/pos/scan").session(session).with(user).with(csrf())
                        .param("barcode", "7700000000001").param("quantity", "1"))
                .andExpect(redirectedUrl("/pos"));
    }
}
