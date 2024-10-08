package org.example.jvspringbootfirstbook.service.order;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.example.jvspringbootfirstbook.dto.order.OrderDto;
import org.example.jvspringbootfirstbook.dto.order.OrderItemDto;
import org.example.jvspringbootfirstbook.dto.order.PlacingOrderRequestDto;
import org.example.jvspringbootfirstbook.exception.EntityNotFoundException;
import org.example.jvspringbootfirstbook.mapper.OrderItemMapper;
import org.example.jvspringbootfirstbook.mapper.OrderMapper;
import org.example.jvspringbootfirstbook.model.CartItem;
import org.example.jvspringbootfirstbook.model.Order;
import org.example.jvspringbootfirstbook.model.OrderItem;
import org.example.jvspringbootfirstbook.model.ShoppingCart;
import org.example.jvspringbootfirstbook.model.Status;
import org.example.jvspringbootfirstbook.model.User;
import org.example.jvspringbootfirstbook.repository.cart.ShoppingCartRepository;
import org.example.jvspringbootfirstbook.repository.order.OrderRepository;
import org.example.jvspringbootfirstbook.repository.user.UserRepository;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class OrderServiceImpl implements OrderService {
    public static final String MISSING_ORDER_MESSAGE = "Order with your id not found, id: ";
    public static final String MISSING_ORDER_ITEM_MESSAGE
            = "Order item with your id not found, id: ";
    private final OrderRepository orderRepository;
    private final UserRepository userRepository;
    private final ShoppingCartRepository shoppingCartRepository;
    private final OrderItemMapper orderItemMapper;
    private final OrderMapper orderMapper;

    @Override
    public OrderDto makeOrder(PlacingOrderRequestDto requestDto) {
        User user = userRepository.findByShippingAddress(requestDto.shippingAddress())
                .orElseThrow(
                        () -> new EntityNotFoundException("No user found for shipping address "
                                + requestDto.shippingAddress())
                );
        Order newOrder = createEmptyOrder(requestDto, user);

        Order orderWithItems = fillingOrderWithItems(user, newOrder);

        return orderMapper.toDto(orderWithItems);
    }

    private Order fillingOrderWithItems(User user, Order newOrder) {
        ShoppingCart cart = shoppingCartRepository
                .findShoppingCartByUser(user.getId());
        Set<OrderItem> orderItemSet = getOrderItemSet(cart, newOrder);

        Set<OrderItem> orderItemsWithPrice = calculatePriceForSet(orderItemSet);
        newOrder.setOrderItems(orderItemsWithPrice);

        BigDecimal total = orderItemsWithPrice.stream()
                .map(OrderItem::getPrice)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        newOrder.setTotal(total);
        Order save = orderRepository.save(newOrder);
        return save;
    }

    private Order createEmptyOrder(PlacingOrderRequestDto requestDto, User user) {
        Order order = new Order();
        order.setUser(user);
        order.setStatus(Status.PENDING);
        order.setTotal(BigDecimal.ZERO);
        order.setOrderDate(LocalDateTime.now());
        order.setShippingAddress(requestDto.shippingAddress());
        order.setOrderItems(new HashSet<>());
        return orderRepository.save(order);
    }

    private Set<OrderItem> calculatePriceForSet(Set<OrderItem> orderItemSet) {
        return orderItemSet.stream()
                .map(this::setPrice)
                .collect(Collectors.toSet());
    }

    private OrderItem setPrice(OrderItem orderItem) {
        orderItem.setPrice(
                orderItem.getBook().getPrice().multiply(
                        BigDecimal.valueOf(orderItem.getQuantity())
                )
        );
        return orderItem;
    }

    private Set<OrderItem> getOrderItemSet(ShoppingCart cart, Order emptyOrder) {
        Set<CartItem> cartItems = cart.getCartItems();
        return cartItems.stream()
                .map(orderItemMapper::fromCarttoOrderItem)
                .map(orderItem -> setOrderToItem(orderItem, emptyOrder))
                .collect(Collectors.toSet());
    }

    private OrderItem setOrderToItem(OrderItem orderItem, Order emptyOrder) {
        orderItem.setOrder(emptyOrder);
        return orderItem;
    }

    @Override
    public List<OrderDto> getHistory(User user, Pageable pageable) {
        return orderRepository.findAllByUser(user, pageable).stream()
                .map(orderMapper::toDto)
                .toList();
    }

    @Override
    public List<OrderItemDto> getFromOrder(Long orderId) {
        Order order = getOrderFromId(orderId);
        return order.getOrderItems().stream()
                .map(orderItemMapper::toDto)
                .toList();
    }

    @Override
    public OrderItemDto getFromOrder(Long orderId, Long id) {
        Order order = getOrderFromId(orderId);
        Set<OrderItem> orderItems = order.getOrderItems();
        OrderItem orderItem = orderItems.stream()
                .filter(item -> item.getId().equals(id))
                .findFirst()
                .orElseThrow(
                        () -> new EntityNotFoundException(
                                MISSING_ORDER_ITEM_MESSAGE + id
                        )
                );
        return orderItemMapper.toDto(orderItem);
    }

    private Order getOrderFromId(Long orderId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(
                        () -> new EntityNotFoundException(
                                MISSING_ORDER_MESSAGE + orderId)
                );
        return order;
    }

    @Override
    public OrderDto updateStatus(Long id) {
        Order order = orderRepository.findById(id)
                .orElseThrow(
                        () -> new EntityNotFoundException(MISSING_ORDER_MESSAGE + id)
                );
        switch (order.getStatus()) {
            case PENDING:
                order.setStatus(Status.DELIVERED);
                break;
            case DELIVERED:
                order.setStatus(Status.COMPLETED);
                break;
            default:
                order.setDeleted(true);
                break;
        }
        return saveAndMapToDto(order);
    }

    private OrderDto saveAndMapToDto(Order order) {
        return orderMapper.toDto(orderRepository.save(order));
    }
}
