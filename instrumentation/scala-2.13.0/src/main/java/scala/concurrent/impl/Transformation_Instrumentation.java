/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package scala.concurrent.impl;

import com.newrelic.agent.bridge.AgentBridge;
import com.newrelic.agent.bridge.Transaction;
import com.newrelic.api.agent.Segment;
import com.newrelic.api.agent.Token;
import com.newrelic.api.agent.Trace;
import com.newrelic.api.agent.weaver.NewField;
import com.newrelic.api.agent.weaver.Weave;
import com.newrelic.api.agent.weaver.Weaver;
import scala.Function1;
import scala.concurrent.ExecutionContext;
import scala.util.Try;

import java.util.concurrent.atomic.AtomicInteger;

import static com.nr.agent.instrumentation.scala.ScalaUtils.scalaFuturesAsSegments;

@Weave(originalName = "scala.concurrent.impl.Promise$Transformation")
public class Transformation_Instrumentation<T>  {
//    private final Function1<Try<?>, Object> onComplete = Weaver.callOriginal();
    private Try<T> _arg = Weaver.callOriginal();
    @NewField
    private Token token;
    @NewField
    private AgentBridge.TokenAndRefCount tokenAndRefCount;

    private Transformation_Instrumentation(final Function1 _fun, final ExecutionContext _ec, final Try<T> _arg, final int _xform)  {
        Transaction transaction = AgentBridge.getAgent().getTransaction(false);
        if (AgentBridge.activeToken.get() == null &&
                transaction != null &&
                AgentBridge.getAgent().getTracedMethod().isTrackCallbackRunnable()) {
            token = transaction.getToken();
        }
    }

    /**
     * Override the submitWithValue so we can replace it with our wrapped version.
     * It plays similar role to the 2.12 setter for "value" the old CallbackRunnable
     */
    @Trace(async = true, excludeFromTransactionTrace = true)
    public final Promise.Transformation submitWithValue(final Try<T> resolved) {
        if (token != null) {
            token.linkAndExpire();
            token = null;
        }
        AgentBridge.TokenAndRefCount tokenAndRefCount = AgentBridge.activeToken.get();
        if (tokenAndRefCount == null) {
            Transaction tx = AgentBridge.getAgent().getTransaction(false);
            if (tx != null) {
                this.tokenAndRefCount = new AgentBridge.TokenAndRefCount(tx.getToken(),
                        AgentBridge.getAgent().getTracedMethod(), new AtomicInteger(1));
            }
        } else {
            this.tokenAndRefCount = tokenAndRefCount;
            this.tokenAndRefCount.refCount.incrementAndGet();
        }
        return Weaver.callOriginal();
    }

    @Trace(excludeFromTransactionTrace = true)
    public void run() {
        Segment segment = null;
        boolean remove = false;

        if (tokenAndRefCount != null) {
            // If we are here and there is no activeToken in progress we are the first one so we set this boolean in
            // order to correctly remove the "activeToken" from the thread local after the original run() method executes
            remove = AgentBridge.activeToken.get() == null;
            AgentBridge.activeToken.set(tokenAndRefCount);
            if (scalaFuturesAsSegments && remove) {
                Transaction tx = AgentBridge.getAgent().getTransaction(false);
                if (tx != null) {
                    segment = tx.startSegment("Scala", "Callback");
                    segment.setMetricName("Scala", "Callback",
                            "IDK");//ScalaUtils.nameScalaFunction(onComplete.getClass().getName()));
                }
            }
        }

        try {
            Weaver.callOriginal();
        } finally {
            if (tokenAndRefCount != null) {
                if (segment != null) {
                    segment.end();
                }

                if (remove) {
                    AgentBridge.activeToken.remove();
                }

                if (tokenAndRefCount.refCount.decrementAndGet() == 0) {
                    tokenAndRefCount.token.expire();
                    tokenAndRefCount.token = null;
                }
            }
        }
    }

}
