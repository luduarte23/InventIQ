package co.edu.unal.inventiq.domain.model;

import static co.edu.unal.inventiq.fixtures.ProductBuilder.aProduct;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

import co.edu.unal.inventiq.domain.exception.InvalidQuantityException;

class CartTest {

    private static final Product YOGURT = product(1, "7702001000011", "Yogur", 3200);
    private static final Product MILK = product(2, "7702001000028", "Leche", 4500);

    @Test
    void newCartIsEmpty() {
        Cart cart = new Cart();

        assertThat(cart.isEmpty()).isTrue();
        assertThat(cart.total()).isEqualByComparingTo("0");
        assertThat(cart.unitCount()).isZero();
    }

    @Test
    void scanningTheSameProductTwiceAddsToTheSameLine() {
        Cart cart = new Cart();

        cart.add(YOGURT, 1);
        cart.add(YOGURT, 1);

        assertThat(cart.lines()).hasSize(1);
        assertThat(cart.quantityOf(YOGURT.id())).isEqualTo(2);
        assertThat(cart.lines().get(0).subtotal()).isEqualByComparingTo("6400");
    }

    @Test
    void totalAndUnitCountAddUpAllLines() {
        Cart cart = new Cart();

        cart.add(YOGURT, 2);
        cart.add(MILK, 3);

        assertThat(cart.total()).isEqualByComparingTo("19900");
        assertThat(cart.unitCount()).isEqualTo(5);
        assertThat(cart.lines()).extracting(Cart.CartLine::productName).containsExactly("Yogur", "Leche");
    }

    @Test
    void removeDeletesTheWholeLine() {
        Cart cart = new Cart();
        cart.add(YOGURT, 2);
        cart.add(MILK, 1);

        cart.remove(YOGURT.id());

        assertThat(cart.quantityOf(YOGURT.id())).isZero();
        assertThat(cart.lines()).hasSize(1);
    }

    @Test
    void quantityMustBePositive() {
        Cart cart = new Cart();

        assertThatThrownBy(() -> cart.add(YOGURT, 0)).isInstanceOf(InvalidQuantityException.class);
        assertThat(cart.isEmpty()).isTrue();
    }

    @Test
    void linesCannotBeModifiedFromOutside() {
        Cart cart = new Cart();
        cart.add(YOGURT, 1);

        assertThatThrownBy(() -> cart.lines().clear()).isInstanceOf(UnsupportedOperationException.class);
    }

    private static Product product(long id, String barcode, String name, int price) {
        return aProduct().id(id).barcode(barcode).name(name).salePrice(price).cost(1).build();
    }
}
