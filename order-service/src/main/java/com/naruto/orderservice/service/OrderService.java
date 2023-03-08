package com.naruto.orderservice.service;

import com.naruto.orderservice.dto.InventroyResponse;
import com.naruto.orderservice.dto.OrderLineItemsDto;
import com.naruto.orderservice.dto.OrderRequest;
import com.naruto.orderservice.model.Order;
import com.naruto.orderservice.model.OrderLineItems;
import com.naruto.orderservice.repository.OrderRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.Arrays;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor

public class OrderService {

    private final OrderRepository orderRepository;
    private final WebClient.Builder webClientBuilder;

    public void placeOrder(OrderRequest orderRequest) {
        Order order = new Order();
        order.setOrderNumber(UUID.randomUUID().toString());

        List<OrderLineItems> orderLineItems = orderRequest.getOrderLineItemsDtoList()
                .stream()
                .map(this::mapToDto)
                .toList();

        order.setOrderLineItemsList(orderLineItems);

        List<String> skuCodes = order.getOrderLineItemsList().stream()
                .map(OrderLineItems::getSkuCode)
                .toList();

        //call ineventory service

        InventroyResponse[] inventroyResponsArry = webClientBuilder.build().get()
                .uri("http://inventory-service/api/inventory",
                        uriBuilder -> uriBuilder.queryParam("skuCode", skuCodes).build())
                .retrieve()
                .bodyToMono(InventroyResponse[].class)
                .block();
      boolean allProductsInStock =  Arrays.stream(inventroyResponsArry).allMatch(InventroyResponse::isInStock);

        if (allProductsInStock) {

            orderRepository.save(order);
        } else {
            throw new IllegalArgumentException("Product is not in stock please try again ");
        }
    }

    private OrderLineItems mapToDto(OrderLineItemsDto orderLineItemsDto) {
        OrderLineItems orderLineItems = new OrderLineItems();
        orderLineItems.setPrice(orderLineItemsDto.getPrice());
        orderLineItems.setQuantity(orderLineItemsDto.getQuantity());
        orderLineItems.setSkuCode(orderLineItemsDto.getSkuCode());
        return orderLineItems;
    }
}
