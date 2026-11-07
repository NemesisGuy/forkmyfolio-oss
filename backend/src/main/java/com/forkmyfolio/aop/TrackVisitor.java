package com.forkmyfolio.aop;

import com.forkmyfolio.model.enums.VisitorStatType;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface TrackVisitor {
    VisitorStatType value();
}