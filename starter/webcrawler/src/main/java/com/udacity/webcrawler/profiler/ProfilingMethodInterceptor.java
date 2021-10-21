package com.udacity.webcrawler.profiler;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Objects;

/**
 * A method interceptor that checks whether {@link Method}s are annotated with the {@link Profiled}
 * annotation. If they are, the method interceptor records how long the method invocation took.
 */
final class ProfilingMethodInterceptor implements InvocationHandler {

    private final Object target;
    private final Clock clock;
    private final ProfilingState state;

    // You will need to add more instance fields and constructor arguments to this class.
    ProfilingMethodInterceptor(Object target, Clock clock, ProfilingState profilingState) {
        this.target = Objects.requireNonNull(target);
        this.clock = Objects.requireNonNull(clock);
        this.state = Objects.requireNonNull(profilingState);
    }

    @Override
    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
        //       This method interceptor should inspect the called method to see if it is a profiled
        //       method. For profiled methods, the interceptor should record the start time, then
        //       invoke the method using the object that is being profiled. Finally, for profiled
        //       methods, the interceptor should record how long the method call took, using the
        //       ProfilingState methods.

        Object result;
        Instant start = null;
        boolean isProfiled = isMethodProfiled(method);

        if (isProfiled) {
            start = clock.instant();
        }

        try {
            result = method.invoke(target, args);
        } catch (Throwable t) {
            throw t.getCause();
        } finally {
            if (isProfiled) {
                Duration duration = Duration.between(start, clock.instant());
                state.record(target.getClass(), method, duration);
            }
        }
        return result;
    }

    private boolean isMethodProfiled(Method method) {
        return method.getAnnotation(Profiled.class) != null;
    }
}
