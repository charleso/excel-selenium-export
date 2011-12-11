package com.mincom.exceltest
import Option.{ apply => ? }
import scala.collection.JavaConversions._
import java.io.FileInputStream
import org.apache.poi.hssf.usermodel.HSSFWorkbook

object Generator {

  def main(args: Array[String]) {
    Config.testResources = "/opt/home/co4222/Desktop/ellipseautomation/Ellipse8.2/TestResources"
    Config.dataFile = "TestData_MAT01.xls"
    val steps = parse("/home/co4222/Desktop/SerialTracking-MSE100-CreateCatalogue(II).xls");
    println(generate("SerialTracking", new Datastore(), steps))
  }

  def parse(file: String) = {
    val myxls = new FileInputStream(file)
    val wb = new HSSFWorkbook(myxls)
    for {
      row <- wb.getSheetAt(0).rowIterator().drop(4)
      if !row.getCell(1).getStringCellValue().isEmpty
    } yield {
      def get(i: Int) = ?(row.getCell(i)).map(_.getStringCellValue()).getOrElse("")
      new Step(get(2).toLowerCase, Description(get(3)), Event(get(4)), parseValues(get(5)), opt(get(6)), get(7) == "Y")
    }
  }

  def opt(a: String) = if (a.isEmpty) None else Some(a)

  def generate(name: String, datastore: Datastore, steps: Iterator[Step]) = {
    val method = steps.map(step => {
      List(
        // step.remark.map("// " + _),
        step.generate(datastore).map(_ + ";"),
        if (Config.screenshots && step.screenshot) List("""mfuiv2.captureScreenshot("test.png");""") else List(),
        List())
    }).flatten.flatten.map("\t\t" + _).mkString("\n")

    """package com.mincom.qtptest;

import com.mincom.ria.automated.AbstractMFUITest;
import com.mincom.appfield.*;

public class %s extends AbstractMFUITest {
        
	public void test() {
%s
	}
}
""".format(name, method)
  }

  def parseValues(value: String): List[Any] = {
    def parseValue(s: String) = {
      if (s.startsWith("Datatable")) {
        new Datatable(s.substring("Datatable(\"".length, s.length - 2))
      } else if (s.startsWith("\"")) {
        s.substring(1, s.length() - 1)
      } else {
        try {
          s.toInt
        } catch {
          case e: NumberFormatException => s
        }
      }
    }
    value.split(",").filter(!_.isEmpty).map(_.trim).map(parseValue).toList
  }

  object Description {
    def apply(s: String) = DescWrapper(s, s match {
      case "QUICKLAUNCH" => QuickLaunch()
      case "TABNAVIGATOR" => TabNavigator()
      case _ => {
        val split = s.split("_").last
        if (s.split("_").lift(2).map(_.startsWith("Menu")).getOrElse(false)) {
          Menu(split.substring(4))
        } else if (split == "ActionMenu") {
          ActionMenu()
        } else if (split == "Message") {
          Message()
        } else {
          Widget(split)
        }
      }
    }, s match {
      case "QUICKLAUNCH" => Unknown()
      case "TABNAVIGATOR" => Unknown()
      case _ => {
        Page(s.split("_")(1))
      }
    })
  }

  case class DescWrapper(full: String, desc: Description, page: Page)

  trait Description
  case class QuickLaunch extends Description
  case class TabNavigator extends Description
  case class Menu(value: String) extends Description
  case class ActionMenu() extends Description
  case class Widget(value: String) extends Description
  case class Message() extends Description

  trait Page {
    def name: String
  }
  case class Search extends Page {
    def name = "Search"
  }
  case class Detail extends Page {
    def name = "Detail"
  }
  case class Unknown extends Page {
    def name = ""
  }

  object Page {
    def apply(value: String) = value match {
      case "Search" => Search()
      case _ => Detail()
    }
  }

  case class Step(app: String, description: DescWrapper, event: Event, value: List[Any], remark: Option[String], screenshot: Boolean) {

    def getVar() = "%s.get%s()" format (app.toLowerCase, description.page.name)

    def getWidget(widget: Widget) = """%s.get%s()""" format (getVar(), Dictionary.parse(app).getOrElse(description.full, "WARNING " + widget.value).capitalize)

    def generate(datastore: Datastore): List[String] = {

      def setValue(widget: Widget) = List("""%s.setValue("%s")""" format (getWidget(widget), datastore(app, value(0))))

      (description.desc, event) match {
        //case (TabNavigator(), _) => List("// TODO Switch to %s" format value(0))
        case (QuickLaunch(), Input()) => {
          val name = value.get(0).toString();
          val a = """%s %s = new %1$s(mfuiv2)""" format (name.toUpperCase, name.toLowerCase)
          //val b = """%3$s.%s %s%1$s = %2$s.get%1$s()""" format ("Search", name.toLowerCase, name.toUpperCase)
          List(a)
        }
        case (Menu("Actions"), Click()) => List()
        case (Menu(_), Select()) => List("""%s.getToolbar().click%s()""" format (getVar(), value(0).toString.replaceAll(" ", "").replaceAll("/", "")))
        case (Menu(menu), Click()) => List("""%s.getToolbar().click%s()""" format (getVar(), Dictionary.parse(app).get(description.full).get))
        //case (Menu("Search"), Click()) => List("""%s.search()""" format getVar())
        case (ActionMenu(), Select()) => List("""%s.getToolbar().click%s()""" format (getVar(), value(0)))
        case (Message(), CheckProperty()) => List("""assertEquals("%s", %s.getMessage())""" format (value(1), getVar()))
        case (widget: Widget, Select()) => setValue(widget)
        case (widget: Widget, Input()) => setValue(widget)
        //case (widget: Widget, DoubleClickCell()) => Some("""grid.doubleClick(%s)""" format (value(0)))
        //case (widget: Widget, Change()) => List("""%s.selectTab("%s")""" format (app, value(0)))
        case (widget: Widget, CheckProperty()) => value(0) match {
          case "visible" => if (!Dictionary.parse(app).get(description.full).get.contains(" ")) List("""%s.assertVisible(%s)""" format (getWidget(widget), value(1) == "True")) else List()
          case "text" => List("""%s.assertValue("%s")""" format (getWidget(widget), datastore(app, value(1))))
          case _ => List()
        }
        case (_, _) => List()
      }
    }
  }

}