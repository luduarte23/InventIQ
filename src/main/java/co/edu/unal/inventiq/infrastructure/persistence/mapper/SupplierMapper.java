package co.edu.unal.inventiq.infrastructure.persistence.mapper;

import co.edu.unal.inventiq.domain.model.MinimumOrder;
import co.edu.unal.inventiq.domain.model.MinimumOrderType;
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
                minimumOrder(entity),
                entity.isActive());
    }

    /** null si el proveedor no tiene pedido mínimo registrado. */
    private static MinimumOrder minimumOrder(SupplierEntity entity) {
        if (entity.getMinOrderType() == null || entity.getMinOrderValue() == null) {
            return null;
        }
        return new MinimumOrder(MinimumOrderType.valueOf(entity.getMinOrderType()), entity.getMinOrderValue());
    }
}
