package com.mincom.exceltest

import java.io.FileInputStream

import org.apache.poi.hssf.usermodel.HSSFWorkbook

import scala.collection.JavaConversions._

object Datatable extends Cache[String, String, String] {

  def _parse(app: String) = {
    val wb = new HSSFWorkbook(new FileInputStream(Config.datatableFile))
    val sheet = wb.getSheet(app.toUpperCase());
    val columns = sheet.getRow(0).cellIterator()
      .map(cell => (cell.getColumnIndex(), cell.getStringCellValue()))
      .toMap
    val x = sheet.getRow(1).cellIterator()
      .map(cell => (columns(cell.getColumnIndex()), cell.toString))
      .toMap
      .mapValues(a => if (a.startsWith("Environment(")) Config.env(a.substring(13, a.length - 2)) else a)
    x
  }

}

class Datastore {
  def apply(app: String, dt: Any) = dt match {
    case Datatable(value) => Datatable.parse(app)(value)
    case _ => dt
  }
}

case class Datatable(value: String)
