package com.paulasmuth.parallel_cf

import scala.collection.JavaConverters._
import scala.collection.mutable.Buffer
import scala.io._
import java.util.concurrent._

class CSVReader[T <: Any](next: (Map[Symbol, Int] => T)){

  var num_threads = 32

  def read(file_path: String) : Buffer[T] = {
    val src = Source.fromFile(file_path)
    var hdl : List[Symbol] = null

    val res = new java.util.LinkedList[Future[T]]()
    val run = Executors.newFixedThreadPool(num_threads)

    (src.getLines().zipWithIndex foreach ((l: (String, Int)) => {

      if (l._2 == 0) 
        hdl = parse_line(l._1) map (Symbol(_))

      else if (hdl != null)
        res.add(run.submit(new Callable[T] { def call = {
          next(parse_line_with_headers(l._1, hdl))
        }}))

    }))

    src.close()

    run.shutdown
    run.awaitTermination(Math.MAX_LONG, TimeUnit.SECONDS)

    res.asScala.map(_.get(0, TimeUnit.SECONDS))
  }


  private def parse_line(line: String) : List[String] =
    line.split(",").toList map (_.replaceAll("^\"(.*)\"$", "$1")) // FIXPAUL HACK!!! :)


  private def parse_line_with_headers(line: String, hdl: List[Symbol]) : Map[Symbol, Int] =
    Map((hdl.zip(parse_line(line)) map (
      (l: (Symbol, String)) => (l._1, Integer.parseInt(l._2))
    )).toArray: _*)

}