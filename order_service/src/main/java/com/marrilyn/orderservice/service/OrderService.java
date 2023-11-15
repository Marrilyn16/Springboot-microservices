package com.marrilyn.orderservice.service;

import com.marrilyn.orderservice.dto.InventoryResponse;
import com.marrilyn.orderservice.dto.OrderLineItemsDto;
import com.marrilyn.orderservice.dto.OrderRequest;
import com.marrilyn.orderservice.model.Order;
import com.marrilyn.orderservice.model.OrderLineItems;
import com.marrilyn.orderservice.repository.OrderRepository;
import io.micrometer.tracing.Span;
import io.micrometer.tracing.Tracer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.reactive.function.client.WebClient;
import zipkin2.internal.Trace;

import java.util.Arrays;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional
@Slf4j
public class OrderService {
    private final OrderRepository orderRepository;
    private final WebClient.Builder webClientBuilder;
    private final Tracer tracer;
    public String placeOrder(OrderRequest orderRequest) {
        Order order = new Order();
        order.setOrderNumber(UUID.randomUUID().toString());

        List<OrderLineItems> orderLineItems = orderRequest.getOrderLineItemsDtoList()
                .stream()
                .map(this::mapToDto)
                .toList();
        order.setOrderLineItemsList(orderLineItems);

        List<String> skuCodes = order.getOrderLineItemsList()
                .stream()
                .map(OrderLineItems::getSkuCode)
                .toList();
        log.info("connect");
        /**** Creating personalized spans *****/
     Span inventoryServiceLookup = tracer.nextSpan().name("InventoryServiceLookup");

     try (Tracer.SpanInScope spanInScope = tracer.withSpan(inventoryServiceLookup.start())){
         //call inventory service and place order if product is in stock
         InventoryResponse[] inventoryResponseArray = webClientBuilder.build().get()
                 .uri("http://inventory-service/api/inventory",
                         uriBuilder -> uriBuilder.queryParam("skuCode", skuCodes).build())
                 .retrieve()
                 .bodyToMono(InventoryResponse[].class)
                 .block();
         log.info("finish");

         Boolean allProductsInStock = Arrays.stream(inventoryResponseArray).allMatch(InventoryResponse::isInStock);

         if(allProductsInStock){
             orderRepository.save(order);
             return "order has been placed";
         } else {
             throw new IllegalArgumentException("Product is not in stock, please try again later");
         }
     }finally {
         inventoryServiceLookup.end();
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
