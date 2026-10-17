package co.edu.unal.inventiq.domain.exception;

public class OrderWithoutSupplierException extends BusinessException {

    public OrderWithoutSupplierException() {
        super("Este pedido está por revisar: asigna un proveedor a sus productos antes de aprobarlo.");
    }
}
