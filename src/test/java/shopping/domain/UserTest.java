package shopping.domain;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatNoException;

class UserTest {

    @Test
    @DisplayName("사용자는 id, email, 비밀번호를 가지고 있다.")
    void createUser() {
        assertThatNoException().isThrownBy(() -> new User(1L, "hello@email.com", "12345"));
    }

    @Test
    @DisplayName("장바구니에 상품을 추가한다.")
    void addCartItem() {
        // given
        User user = new User();
        Product product = new Product();

        // when
        CartItem item = user.addCartItem(product);

        // then
        assertThat(item.getUser()).isEqualTo(user);
        assertThat(item.getProduct()).isEqualTo(product);
        assertThat(item.getQuantity()).isEqualTo(1);
    }

    @Test
    @DisplayName("장바구니에 존재하는 상품을 추가하면 수량이 늘어난다.")
    void addCartItemDuplicate() {
        // given
        User user = new User();
        Product product = new Product();

        // when
        user.addCartItem(product);
        user.addCartItem(product);
        CartItem item = user.addCartItem(product);

        // then
        assertThat(item.getUser()).isEqualTo(user);
        assertThat(user.getCartItems()).hasSize(1);
        assertThat(item.getProduct()).isEqualTo(product);
        assertThat(item.getQuantity()).isEqualTo(3);
    }
}