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
      List(step.remark.map(_ => ""),
        // step.remark.map("// " + _),
        step.generate(datastore).map(_ + ";"),
        if (Config.screenshots && step.screenshot) Some("""mfuiv2.captureScreenshot("test.png");""") else None,
        None)
    }).flatten.flatten.map("\t\t" + _).mkString("\n")

    """package com.mincom.qtptest;

import com.mincom.ria.automated.AbstractMFUITest;
import com.mincom.ellipse.rc.apiv2.*;

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
      case _ => {
        val split3 = s.split("_")(2)
        val split = s.split("_").last
        if (split3.startsWith("Menu")) {
          Menu(split.substring(4))
        } else if (split == "ActionMenu") {
          ActionMenu()
        } else if (split == "Message") {
          Message()
        } else {
          Widget(split)
        }
      }
    })
  }

  case class DescWrapper(full: String, desc: Description)

  trait Description
  case class QuickLaunch extends Description
  case class Menu(value: String) extends Description
  case class ActionMenu extends Description
  case class Widget(value: String) extends Description
  case class Message extends Description

  case class Step(app: String, description: DescWrapper, event: Event, value: List[Any], remark: Option[String], screenshot: Boolean) {

    def getWidget(widget: Widget) = """%s.getWidget("%s")""" format (app, Dictionary.parse(app).getOrElse(description.full, "WARNING " + widget.value))

    def generate(datastore: Datastore) = {

      def setValue(widget: Widget) = Some("""%s.setValue("%s")""" format (getWidget(widget), datastore(app, value(0))))

      (description.desc, event) match {
        case (QuickLaunch(), Input()) => Some("""Application %s = mfuiv2.loadApp("%1$s")""" format value.get(0).toString().toLowerCase)
        case (Menu("Actions"), Click()) => None
        case (Menu("New"), Click()) => Some("""%s.clickNew()""" format app)
        case (Menu("Search"), Click()) => Some("""%s.search()""" format app)
        case (ActionMenu(), Select()) => Some("""%s.toolbarAction("%s")""" format (app, value(0)))
        case (Message(), CheckProperty()) => Some("""assertEquals("%s", %s.getErrorMessages().get(0))""" format (value(1), app))
        case (widget: Widget, Select()) => setValue(widget)
        case (widget: Widget, Input()) => setValue(widget)
        //case (widget: Widget, DoubleClickCell()) => Some("""grid.doubleClick(%s)""" format (value(0)))
        case (Widget(_), Change()) => Some("""%s.selectTab("%s")""" format (app, value(0)))
        case (widget: Widget, CheckProperty()) => value(0) match {
          case "visible" => Some("""%s.assertVisible(%s)""" format (getWidget(widget), value(1) == "True"))
          case "text" => Some("""%s.assertValue("%s")""" format (getWidget(widget), datastore(app, value(1))))
          case _ => None
        }
        case (_, _) => None
      }
    }
  }

}