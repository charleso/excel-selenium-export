package com.mincom.exceltest

object Event {
  def apply(value: String): Event = value match {
    case "Input" => Input()
    case "Type" => Type()
    case "Wait" => Wait()
    case "WaitProperty" => WaitProperty()
    case "CheckProperty" => CheckProperty()
    case "Click" => Click()
    case "Change" => Change()
    case "ChangeFocus" => ChangeFocus()
    case "CloseApplications" => CloseApplications()
    case "Exist" => Exist()
    case "Select" => Select()
    case "Save" => Save()
    case "Open" => Open()
    case "Show" => Show()
    case "FindRowScroll" => FindRowScroll()
    case "DoubleClickCell" => DoubleClickCell()
    case _ => throw new RuntimeException("Invalid event: " + value)
  }
}
trait Event

case class Input extends Event
case class Type extends Event
case class Wait extends Event
case class WaitProperty extends Event
case class CheckProperty extends Event
case class Click extends Event
case class Change extends Event
case class ChangeFocus extends Event
case class CloseApplications extends Event
case class Exist extends Event
case class Select extends Event
case class Save extends Event
case class Open extends Event
case class Show extends Event
case class FindRowScroll extends Event
case class DoubleClickCell extends Event