/*
 * Copyright (c) 2018 Pivotal Software Inc, All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package reactor.rabbitmq;

import com.rabbitmq.client.AlreadyClosedException;
import com.rabbitmq.client.AuthenticationFailureException;
import com.rabbitmq.client.ShutdownSignalException;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.atomic.AtomicLong;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 *
 */
public class ExceptionHandlersTests {

    ExceptionHandlers.RetrySendingExceptionHandler exceptionHandler;

    @Test
    public void retryableExceptions() {
        exceptionHandler = retrySendingExceptionHandler(Collections.singletonMap(Exception.class, true));
        assertTrue(exceptionHandler.shouldRetry(new AlreadyClosedException(new ShutdownSignalException(true, true, null, null))));
        assertFalse(exceptionHandler.shouldRetry(new Throwable()));

        exceptionHandler = retrySendingExceptionHandler(new HashMap<Class<? extends Throwable>, Boolean>() {{
            put(ShutdownSignalException.class, true);
            put(IOException.class, false);
            put(AuthenticationFailureException.class, true);
        }});

        assertTrue(exceptionHandler.shouldRetry(new ShutdownSignalException(true, true, null, null)),
            "directly retryable");
        assertTrue(exceptionHandler.shouldRetry(new AlreadyClosedException(new ShutdownSignalException(true, true, null, null))),
            "retryable from its super-class");
        assertFalse(exceptionHandler.shouldRetry(new IOException()), "not retryable");
        assertTrue(exceptionHandler.shouldRetry(new AuthenticationFailureException("")), "directly retryable");
    }

    @Test
    void retryTimeoutIsReached() {
        exceptionHandler = new ExceptionHandlers.RetrySendingExceptionHandler(
            100L, 10L, Collections.singletonMap(Exception.class, true)
        );
        exceptionHandler.accept(sendContext(() -> {
            throw new Exception();
        }), new Exception());
    }

    @Test
    void retrySucceeds() {
        exceptionHandler = new ExceptionHandlers.RetrySendingExceptionHandler(
            100L, 10L, Collections.singletonMap(Exception.class, true)
        );
        AtomicLong counter = new AtomicLong(0);
        exceptionHandler.accept(sendContext(() -> {
            if (counter.incrementAndGet() < 3) {
                throw new Exception();
            }
            return null;
        }), new Exception());
        assertEquals(3, counter.get());
    }

    private ExceptionHandlers.RetrySendingExceptionHandler retrySendingExceptionHandler(Map<Class<? extends Throwable>, Boolean> retryableExceptions) {
        return new ExceptionHandlers.RetrySendingExceptionHandler(
            100L, 10L, retryableExceptions
        );
    }

    private Sender.SendContext sendContext(Callable<Void> callable) {
        return new Sender.SendContext(null, null) {

            @Override
            public void publish() throws Exception {
                callable.call();
            }
        };
    }
}