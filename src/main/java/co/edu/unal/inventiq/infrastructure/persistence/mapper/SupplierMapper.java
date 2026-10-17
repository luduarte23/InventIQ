package co.edu.unal.inventiq.infrastructure.persistence.mapper;

import co.edu.unal.inventiq.domain.model.Supplier;
import co.edu.unal.inventiq.infrastructure.persistence.entity.SupplierEntity;

public final class SupplierMapper {

    private SupplierMapper() {
    }

    public static Supplier toDomain(SupplierEntity entity) {
        return new Supplier(
                entity.getId(),
                entity.getName(),
                entity.getWhatsapp(),
                entity.getLeadTimeDays(),
                entity.isActive());
    }
}
