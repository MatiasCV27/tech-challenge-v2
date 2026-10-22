package com.challange.order_service.client;

import feign.RequestInterceptor;
import feign.RequestTemplate;
import org.slf4j.MDC;
import org.springframework.stereotype.Component;

@Component
public class FeignClientInterceptor implements RequestInterceptor {

    private static final String TRACE_ID_HEADER = "X-Trace-Id";
    private static final String TRACE_ID_MDC_KEY = "traceId";

    @Override
    public void apply(RequestTemplate template) {
        String traceId = MDC.get(TRACE_ID_MDC_KEY);
        if (traceId != null && !traceId.isEmpty()) {
            template.header(TRACE_ID_HEADER, traceId);
        }
    }
}