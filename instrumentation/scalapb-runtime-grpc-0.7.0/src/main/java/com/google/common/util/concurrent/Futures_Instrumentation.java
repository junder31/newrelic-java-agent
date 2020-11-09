package com.google.common.util.concurrent;

import com.newrelic.api.agent.NewRelic;
import com.newrelic.api.agent.Token;
import com.newrelic.api.agent.Trace;
import com.newrelic.api.agent.weaver.NewField;
import com.newrelic.api.agent.weaver.Weave;
import com.newrelic.api.agent.weaver.Weaver;

import java.util.concurrent.Future;

@Weave(originalName = "com.google.common.util.concurrent.Futures")
public final class Futures_Instrumentation {

    @Weave(originalName = "com.google.common.util.concurrent.Futures$CallbackListener")
    private static final class CallbackListener<V> {
        @NewField
        final Token token;

        CallbackListener(Future<V> future, FutureCallback<? super V> callback) {
            this.token = NewRelic.getAgent().getTransaction().getToken();
        }

        @Trace(async = true, excludeFromTransactionTrace = true)
        public void run() {
            token.linkAndExpire();
            Weaver.callOriginal();
        }
    }
}
