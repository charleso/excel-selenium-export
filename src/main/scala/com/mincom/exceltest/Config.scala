package com.mincom.exceltest

object Config {

  val screenshots = false

  val env = collection.mutable.Map[String, String]()
  var testResources: String = ""
  var dataFile: String = ""
  def dictionaryDir = testResources + "/Libraries/Dicts/"
  def datatableFile = testResources + "/TestDataDriven/" + dataFile
}