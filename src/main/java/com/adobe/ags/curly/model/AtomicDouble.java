package com.adobe.ags.curly.model;

import java.util.concurrent.atomic.AtomicReference;

public class AtomicDouble {
    private AtomicReference<Double> value = new AtomicReference(0.0);

    public final Double getAndAdd(Double delta) {
        while (true) {
            Double currentValue = value.get();
            Double newValue = currentValue + delta;
            if (value.compareAndSet(currentValue, newValue)) {
                return currentValue;
            }
        }
    }
    
    public final Double getAndSet(Double newValue) {
        while (true) {
            Double currentValue = value.get();
            if (value.compareAndSet(currentValue, newValue)) {
                return currentValue;
            }
        }
    }    
    
    public final Double get() {
        return value.get();
    }
}