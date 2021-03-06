/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package anorm

import com.newrelic.api.agent.weaver.{MatchType,Weave,Weaver,NewField}
import com.newrelic.api.agent.weaver.scala.{ScalaWeave, ScalaMatchType}
import com.newrelic.agent.bridge.AgentBridge
import com.newrelic.api.agent.Trace
import com.newrelic.api.agent.TransactionNamePriority
import java.sql.{Connection, ResultSet}
import java.util.logging.Level

import resource.ManagedResource

import scala.util.Try

/**
 * We want to trace on the 'object Sql' in anorm, not the 'trait Sql', which are both defined in the
 * anorm package. In Scala, an 'object' is a approximately a Singleton instance of a type. A 'trait'
 * is approximately like an interface in Java8 with default implementations, providing multiple
 * inheritance of a kind. Unfortunately, Anorm uses name overloading, which makes this a little
 * confusing, to tease out which of Sql$.class, Sql.class or Sql$class.class is desired. In this
 * case, "Sql$.class" maps to the 'object Sql' we want to instrument.
 *
 * Note that weaving against 'object Sql' might not work...
 *
 * Created by thoffman on 9/14/15.
 */
@ScalaWeave(`type` = ScalaMatchType.Object)
class Sql {

  /*
   * In anorm-3.0, the signature of 'object Sql' method 'as[T](...)' changes to private. When compiled with
   * scala 2.10, the compiled method is given a different named.
   */
  @Trace(metricName = "ANorm/ResultSetParsing")
  def asTry[T](parser: ResultSetParser[T], rs: ManagedResource[ResultSet], onFirstRow: Boolean)(implicit connection: Connection): Try[T] = {
    // It would be clever to figure out the SQL statement,
    return Weaver.callOriginal();
  }

}

object NewRelicConstants {
  val ANORM_CONTROLLER_ACTION: String = "AnormControllerAction"
}
