package com.spotify.scio.examples.extra

import com.spotify.scio._
import com.spotify.scio.experimental._


/*
sbt -Dbigquery.secret=/path/to/secret.json -Dbigquery.project=[PROJECT]

runMain
  com.spotify.scio.examples.TypedBigQueryTornadoes
  --project=[PROJECT] --runner=DataflowPipelineRunner --zone=[ZONE]
  --stagingLocation=gs://[BUCKET]/path/to/staging
  --output=[DATASET].typed_bigquery_tornadoes
*/

object TypedBigQueryTornadoes {

  // Annotate input class with schema inferred from a BigQuery SELECT.
  // Class Row will be expanded into a case class with fields from the SELECT query. A companion
  // object will also be generated to provide easy access to original query/table from annotation,
  // TableSchema and converter methods between the generated case class and TableRow.
  @BigQueryType.fromQuery("SELECT tornado, month FROM [publicdata:samples.gsod]")
  class Row

  // Annotate output case class.
  // Note that the case class is already defined and will not be expanded. Only the companion
  // object will be generated to provide easy access to TableSchema and converter methods.
  @BigQueryType.toTable()
  case class Result(month: Long, tornado_count: Long)

  def main(cmdlineArgs: Array[String]): Unit = {
    val (sc, args) = ContextAndArgs(cmdlineArgs)

    // Get input from BigQuery and convert elements from TableRow to Row.
    // SELECT query from the original annotation is used by default.
    sc.typedBigQuery[Row]()
      .flatMap(r => if (r.tornado.getOrElse(false)) Seq(r.month) else Nil)
      .countByValue()
      .map(kv => Result(kv._1, kv._2))
      // Convert elements from Result to TableRow and save output to BigQuery.
      .saveAsTypedBigQuery(args("output"), CREATE_IF_NEEDED, WRITE_TRUNCATE)

    sc.close()
  }

}