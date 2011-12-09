package com.mincom.exceltest

trait Cache[A, V, K] {

  val cache = collection.mutable.Map[A, Map[K, V]]()

  def parse(file: A) = cache.getOrElseUpdate(file, _parse(file))

  def _parse(file: A): Map[K, V]

}