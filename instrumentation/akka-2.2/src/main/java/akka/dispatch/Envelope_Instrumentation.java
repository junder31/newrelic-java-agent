/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package akka.dispatch;

import akka.actor.ActorRef;
import com.newrelic.api.agent.Token;
import com.newrelic.api.agent.weaver.NewField;
import com.newrelic.api.agent.weaver.Weave;

@Weave(originalName = "akka.dispatch.Envelope")
public abstract class Envelope_Instrumentation {
    @NewField
    public Token token;

    public abstract Object message();

    public abstract ActorRef sender();
}
