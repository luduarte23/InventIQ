package com.inventiq.application.admin;

import java.util.List;

import com.inventiq.domain.model.Alert;

/** Bandeja de alertas del administrador (D3): lo que ve en su página de inicio. */
public interface AlertInboxService {

    /** Alertas sin resolver, de la más reciente a la más antigua. */
    List<Alert> unresolvedAlerts();
}