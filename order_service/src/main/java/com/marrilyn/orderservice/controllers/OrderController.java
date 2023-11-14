package com.marrilyn.orderservice.controllers;

import com.marrilyn.orderservice.dto.OrderRequest;
import com.marrilyn.orderservice.service.OrderService;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/order")
@RequiredArgsConstructor
public class OrderController {

    private final OrderService orderService;
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @CircuitBreaker(name = "inventory", fallbackMethod = "fallbackMethod")
    public String placeOrder(@RequestBody OrderRequest orderRequest) {
        orderService.placeOrder(orderRequest);
        return "order has been placed";
    }

    public String fallbackMethod(OrderRequest orderRequest, RuntimeException runtimeException){

        return "oops! something went wrong, please retry after 5 minutes!";

    }

}
