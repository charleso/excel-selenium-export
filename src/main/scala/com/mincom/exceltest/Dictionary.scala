package com.mincom.exceltest
import util.parsing.combinator._
import scala.io.Source
import java.io.File
import scala.io.Codec

object Dictionary extends Cache[String, String, String] {

  val NameR = """^\.Add "(.*?)".*automationname:=(.*?)".*?$""".r

  override def _parse(file: String) = {
    Source.fromFile(new File(Config.dictionaryDir + file.toUpperCase + ".vbs"))(Codec.string2codec("UTF-16")).getLines
      .map(_.trim)
      .filter(_.startsWith(".Add"))
      .map(_ match {
        case NameR(value, comp) => Some((value, comp))
        case _ => None
      })
      .flatten
      .map(a => (a._1, a._2.replaceAll("\\^?(.*?)\\$?", "")))
      .map(a => (a._1, a._2.replaceAll("component_", "")))
      .map(a => (a._1, a._2.replaceAll("_numText$", "")))
      .toMap
  }
}

