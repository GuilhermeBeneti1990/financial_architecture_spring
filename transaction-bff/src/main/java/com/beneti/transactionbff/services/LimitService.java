package com.beneti.transactionbff.services;

import com.beneti.transactionbff.dto.DiaryLimit;
import com.beneti.transactionbff.feign.DiaryLimitClient;
import io.github.resilience4j.circuitbreaker.CallNotPermittedException;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.decorators.Decorators;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.function.Supplier;

@Service
public class LimitService {

    DiaryLimitClient diaryLimitClient;

    @Autowired
    private CircuitBreaker countCircuitBreaker;

    public LimitService(DiaryLimitClient diaryLimitClient) {
        this.diaryLimitClient = diaryLimitClient;
    }

    public Mono<DiaryLimit> getDiaryLimit(final Long agency, final Long account) {

        return getDiaryLimitSupplier(agency, account);

    }

    private Mono<DiaryLimit> getDiaryLimitSupplier(final Long agency, final Long account) {
        var diaryLimitSup = countCircuitBreaker.decorateSupplier(() ->
                diaryLimitClient.getDiaryLimit(agency, account)
        );

        return Mono.fromSupplier(Decorators
                    .ofSupplier(diaryLimitSup)
                    .withCircuitBreaker(countCircuitBreaker)
                    .withFallback(Arrays.asList(CallNotPermittedException.class),
                            e -> this.getStaticLimit())
                    .decorate()
        );


    }

    private DiaryLimit getStaticLimit() {
        DiaryLimit diaryLimit = new DiaryLimit();
        diaryLimit.setValue(BigDecimal.ZERO);
        return diaryLimit;
    }

}
