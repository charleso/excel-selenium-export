package com.mincom.exceltest

import scala.xml._

object ScreenParser extends Cache[String, String, String] {

  def main(args: Array[String]) {
    println(parse("mse100"))
  }

  override def _parse(app: String) = _parse(app, "search") ++ _parse(app, "detail")

  def _parse(app: String, t: String) = {
    val xml = XML.load(ScreenParser.getClass().getResourceAsStream("/com/mincom/ellipse/screen/definition/%s%s.xml" format (app.toLowerCase, t)))
    (xml descendant_or_self)
      .filter(node => !(node \ "@label").isEmpty)
      .map(node => (node \ "@label" toString, node \ "@id" toString))
      .map(a => (a._1.split("\\.").map(_.toLowerCase).mkString, a._2))
      .toMap
  }
}