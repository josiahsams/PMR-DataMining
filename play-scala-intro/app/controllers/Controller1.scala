package controllers

import java.io.{File, PrintWriter}
import javax.inject.Inject

import org.apache.spark.sql.SQLContext
import org.apache.spark.{SparkConf, SparkContext}
import play.api.libs.json
import play.api.libs.json.{Json, Writes}
import play.api.mvc.{Action, Controller}

import scala.io.Source
import scala.util.Try

/**
  * Created by sankar on 18/3/16.
  */

class Controller1 @Inject() extends Controller {

  // Spark Context Creation
  val sparkConf = new SparkConf().setAppName("Geo location").setMaster("local")
  val sc = new SparkContext(sparkConf)
  val sqlContext = new SQLContext(sc)

  var i = 0
  // Query based on Geographic Location
  // serializing sql output data to convert it into JSON
  case class FileGeo(Id: Int, CountryName : String, count : Int)

  implicit val geolocation = new Writes[FileGeo] {
    def writes(location: FileGeo) = Json.obj(
      "Id" -> location.Id,
      "CountryName" -> location.CountryName ,
      "Count" -> location.count
    )
  }

  // data frame path
  var dataFile = "../Json-Data/new_pmr_data.json"

  val df = sqlContext.read.json(dataFile)

  df.registerTempTable("df")

  // Spark SQL query
  val c_count1 = sqlContext.sql("SELECT substr(PMR,11,3) , count(1) as cnt from df where PMR <> 'null' group by substr(PMR,11,3) order by cnt desc")
  // caching the data collected
  c_count1.cache().collect()

  def geoLoc = Action {
      implicit request =>
      // variable to store Result

        var geoRes = "["

        // country codes
        val c_codes = collection.mutable.Map[String, String]()
        dataFile = "../Json-Data/country.codes"

        for(line <- Source.fromFile(dataFile).getLines()) {
          val code = line.split(" ", 2)
          val code_name = code.toList
          c_codes += code_name(0) -> code_name(1)
        }

        val op = collection.mutable.Map[String, Int]()
        val q_listg = c_count1.collect()

        i = 0
        for(i <- 0 to q_listg.length -1 ) {
          if (c_codes.contains((q_listg(i)(0)).toString)) {
            op += c_codes((q_listg(i)(0)).toString) -> ((q_listg(i)(1).toString)).toInt
          }
        }
        i = 0
        for ((key, value) <- op.toSeq.sortBy(-_._2)) {
          geoRes += Json.toJson(FileGeo(i+1, q_listg(i)(0).toString, (q_listg(i)(1).toString).toInt))

          geoRes += ","
          i += 1
        }
        geoRes += "]"

      Ok(geoRes)
  }
      // Pervasive Issues
   case class FilePerv(Id: Int, APAR : String, count : Int)

   implicit val perIssue = new Writes[FilePerv] {
     def writes(location: FilePerv) = Json.obj(
       "Id" -> location.Id,
       "APAR" -> location.APAR ,
       "Count" -> location.count
     )
   }

     val dataFile4 = "../Json-Data/pmr_data.json"

     val df4 = sqlContext.read.json(dataFile4)
     df4.registerTempTable("df4")

     val apars = sqlContext.sql("SELECT APAR , count(1) as cnt from df4 where ( APAR REGEXP '[0-9]' ) group by APAR order by cnt desc limit 30")
     val q_listp = apars.cache().collect
  
     def pervasive = Action {
       implicit request =>

         var pervasiveRes = "["
         i = 0
         for(i <- 0 to q_listp.length -1 ) {
           pervasiveRes += Json.toJson(FilePerv(i+1, q_listp(i)(0).toString, (q_listp(i)(1).toString).toInt))
           if(i<q_listp.length-1)
             pervasiveRes += ","
         }

         pervasiveRes += "]"

         Ok(pervasiveRes)
     }

   //  Query for  Codefixratio

  // Serialization of data
  case class FileCode(Id: Int, PMRType : String, Count : Int)
   implicit val codefix = new Writes[FileCode] {
     def writes(location: FileCode) = Json.obj(
        "Id" -> location.Id,
       "PMRType" -> location.PMRType ,
       "Count" -> location.Count
     )
   }

  val dataFile1 = "../Json-Data/pmr_data.json"
  val df1 = sqlContext.read.json(dataFile1)
  df1.registerTempTable("df1")

  val apar_count = sqlContext.sql("SELECT COUNT (DISTINCT APAR), count(*) from df1 where ( APAR REGEXP '[0-9]' ) ")
  val pmr_count = sqlContext.sql("SELECT COUNT(*)  from df1")
  val a_list = apar_count.cache().collect()
  val p_list = pmr_count.cache().collect()

   def codeFixRatio = Action {
      implicit request =>
        i=0
        var codeRes = "["
        codeRes += Json.toJson(FileCode(i+1, "Total PMRs", (p_list(0)(0).toString).toInt))+","
        codeRes += Json.toJson(FileCode(i+2, "Fixed PMRs", (a_list(0)(0).toString).toInt))+"]"

       Ok(codeRes)
       }

     // HardWare related Query

     val dataFile2 = "../Json-Data/pmr_data.json"
     val df2 = sqlContext.read.json(dataFile2)
     df2.registerTempTable("df2")
     val h_count = sqlContext.sql("SELECT `CPU Type` , count(1) from df2 where `CPU Type` <> 'null' group by `CPU Type`  order by `CPU Type`")
     val q_listh = h_count.cache().collect()

    case class FileHard(Id: Int, Hardware : String, Count : Int)
    implicit val hardwar = new Writes[FileHard] {
      def writes(location: FileHard) = Json.obj(
        "Id" -> location.Id,
        "HardWare" -> location.Hardware ,
        "Count" -> location.Count
      )
    }

  def hardware = Action {
       implicit request =>
         import scala.io.Source

         var hardRes = "["
         val h_codes = collection.mutable.Map[String, String]()
         for(line <- Source.fromFile("../Json-Data/hardware.codes").getLines()) {
           val code = line.split(" ", 2)
           val code_name = code.toList
           h_codes += code_name(0) -> code_name(1)
         }

         val op = collection.mutable.Map[String, Int]("Power5" -> 0, "Power6" -> 0, "Power7" -> 0, "Power8" -> 0, "Invalid" -> 0)
         i = 0
         for(i <- 0 to q_listh.length -1 ) {
           if (h_codes.contains((q_listh(i)(0)).toString)) {
             val h_code_val = h_codes(q_listh(i)(0).toString)
             val count = (q_listh(i)(1).toString).toInt
             val c_count = (op(h_code_val.toString)).toInt
             op.update(h_code_val, (c_count+count))
           } else {
             val count = (q_listh(i)(1).toString).toInt
             val c_count = op("Invalid")
             op.update("Invalid" , (count+c_count))
           }
         }
         
         i = 0
         for ((key, value) <- op.toSeq.sortBy(-_._2)) {
           //jsn += "{\"HardWare\":\""+key+"\""+","+"\"Count\":"+value+"}"
            hardRes += Json.toJson(FileHard(i+1, key.toString, (value.toString).toInt))
            hardRes += ","
           i += 1
         }
            hardRes += "]"
         Ok(hardRes)
     }

     // Monthly reports

    case class FileMonth(id: Int, Month: String, count: Int)

    implicit val monthlyrep = new Writes[FileMonth] {
      def writes(location: FileMonth) = Json.obj(
        "Id" -> location.id,
        "Month" -> location.Month,
        "Count" -> location.count
      )
    }

  val dataFile3 = "../Json-Data/pmr_data.json"
  val df3 = sqlContext.read.json(dataFile3)
  df3.registerTempTable("df3")

  val m_count = sqlContext.sql("SELECT substr(`Created Date`, 5, 2), count(1) as cnt from df3 where `Created Date` <> 'null' group by substr(`Created Date`, 5, 2)")
  val q_listm = m_count.cache().collect

     def monthly = Action {
       implicit request =>

         var monthRes = "["
         val months = collection.mutable.Map[String, String]("01" -> "Jan", "02" -> "Feb", "03" -> "Mar", "04" -> "Apr", "05" -> "May", "06" -> "Jun", "07" -> "Jul", "08" -> "Aug", "09" -> "Sep", "10" -> "Oct", "11" -> "Nov", "12" -> "Dec")
         val op = collection.mutable.Map[String, Int]()
         i = 0
         for(i <- 0 to q_listm.length -1 ) {
           if (months.contains((q_listm(i)(0)).toString)) {
             op += months((q_listm(i)(0)).toString) -> ((q_listm(i)(1).toString)).toInt
           }
         }

         val a_m = Array("Jan", "Feb", "Mar", "Apr", "May", "Jun", "Jul", "Aug", "Sep", "Oct", "Nov", "Dec")
          var j = 0
         for(i <- 0 to 11) {
           val mm = a_m(i)
           if (op.contains(mm)) {
             val cc = op(mm)
             // jsn += "{\"Month\":\""+mm+"\""+","+"\"Count\":"+cc+"}"
             monthRes += Json.toJson(FileMonth(j+1, mm.toString, (cc.toString).toInt))+","
           } else {
             val cc = 0
             //jsn += "{\"Month\":\""+mm+"\""+","+"\"Count\":"+cc+"}"
             monthRes += Json.toJson(FileMonth(i+1, mm.toString, (cc.toString).toInt))+","
           }
           j = j+1
         }
            monthRes += "]"
         Ok(monthRes)
     }


         // Sentiment Analysis

     case class FileSenti(id: Int, sentimentType: String, count: Int)

       implicit val sentim = new Writes[FileSenti] {
         def writes(location: FileSenti) = Json.obj(
            "Id" -> location.id,
           "SentimentType" -> location.sentimentType ,
           "Count" -> location.count
         )
       }

         def sentiment = Action {
           implicit request =>

             val dataFile5 = "../Json-Data/sowmya.json"
             val df5 = sqlContext.read.json(dataFile5)
             df5.registerTempTable("df5")
             val pmr_count = sqlContext.sql("SELECT PMR_DATA from df5")

             val noval = 0;
             val pos_words = collection.mutable.Map[String, Int]()

             var pos_wordsfile = "../Json-Data/pos-words.txt"
             for(line <- Source.fromFile(pos_wordsfile).getLines()) {
               val words = line.split(" ", 2)
               val  code_name = words.toList
               pos_words += code_name(0) -> noval
             }

             val neg_words = collection.mutable.Map[String, Int]()

             var neg_wordsfile = "../Json-Data/neg-words.txt"
             for(line <- Source.fromFile(neg_wordsfile).getLines()) {
               val words = line.split(" ", 2)
               val  code_name = words.toList
               neg_words += code_name(0) -> noval
             }


             val stop_words = collection.mutable.Map[String, Int]()

             var stop_wordsfile = "../Json-Data/stop-words.txt"
             for(line <- Source.fromFile(stop_wordsfile).getLines()) {
               val words = line.split(" ", 2)
               val  code_name = words.toList
               stop_words += code_name(0) -> noval
             }

             val q_list = pmr_count.collect
             var sentiment = 0;
             var flag =0;
             var pos_pmr = 0;
             var neg_pmr = 0;
             var neu_pmr = 0;

             for(j <- 0 to q_list.length - 1) {
               sentiment = 0
               flag = 0
               val pmr_words = (q_list(j).toString).split(" ")
               val words_list = pmr_words.toList
               for(i <- 0 to words_list.length -1 ) {
                 if (stop_words.contains((words_list(i)).toString)) {
                   flag = 1
                 }
                 else if (pos_words.contains((words_list(i)).toString)) {
                   if (flag == 1) {
                     sentiment -= 1
                     flag = 0
                   } else {
                     sentiment += 1
                   }
                 }
                 else if (neg_words.contains((words_list(i)).toString)) {
                   if (flag == 1) {
                     sentiment += 1
                     flag = 0
                   } else {
                     sentiment -= 1
                   }
                 }
               }
               if(sentiment > 0) {
                 pos_pmr += 1;
               }
               else if(sentiment < 0) {
                 neg_pmr += 1;
               }else {
                 neu_pmr += 1;
               }
             }

             val op = collection.mutable.Map[String, Int]("Positive Sentiment" -> pos_pmr, "Negative Sentiment" -> neg_pmr, "Neutral Sentiment" -> neu_pmr)

             i=0
             var sentiRes = "["
             for ((key, value) <- op.toSeq.sortBy(-_._2)) {
               // jsn += "{\"Sentiment Type\":\""+key+"\""+","+"\"Count\":"+value+"}"
               sentiRes += Json.toJson(FileSenti(i+1, (key.toString), (value.toString).toInt))
               if(i<2)
                 sentiRes += ","
               i = i+1
             }
             sentiRes += "]"

             Ok(sentiRes)
         }

           // Software Reports query
  val dataFile6 = "../Json-Data/software.json"
  val df6 = sqlContext.read.json(dataFile6)
  df6.registerTempTable("df6")

  var rel_adopt = sqlContext.sql("SELECT Product , count(1) from df6 group by Product ")

  val q_listso = rel_adopt.cache().collect

  case class FileSoft(Id: Int, Software : String, Count : Int)
  implicit val softwar = new Writes[FileSoft] {
    def writes(location: FileSoft) = Json.obj(
      "Id" -> location.Id,
      "SoftWare" -> location.Software ,
      "Count" -> location.Count
    )
  }

           def software = Action {
             implicit request =>

               var softRes = "["
               val rel = Map[String, String]("5765G0300" -> "AIX 5.3", "5765G6200" -> "AIX 6.1", "5765H4000" -> "AIX 7.1", "5765G3400" -> "VIOS")

               i = 0
               for(i <- 0 to q_listso.length -1 ) {
                 if (q_listso(i)(0) != null && rel.contains(q_listso(i)(0).toString)) {
                   //jsn += "{\"Software Type\":\""+q_listso(i)(0)+"\""+","+"\"Count\":"+q_listso(i)(1)+"}"
                   softRes += Json.toJson(FileSoft(i+1, q_listso(i)(0).toString, (q_listso(i)(1).toString).toInt))+","

                 } else if (q_listso(i)(0) == null) {
                   //jsn += "{\"Software Type\":\"Invalid\""+","+"\"Count\":"+q_listso(i)(1)+"}"
                   softRes += Json.toJson(FileSoft(i+1, "Invalid", (q_listso(i)(1).toString).toInt))+","
                 }
               }
               softRes += "]"
               Ok(softRes)
           }

           // critsits query

      case class FileCrit(Id: Int, CustomerName : String, Number : Int)
        implicit val critical = new Writes[FileCrit] {
          def writes(location: FileCrit) = Json.obj(
            "Id" -> location.Id,
            "Customer Name" -> location.CustomerName ,
            "Number" -> location.Number
          )
        }

        val dataFile7 = "../Json-Data/pmr_data.json"
        val df7 = sqlContext.read.json(dataFile7)
        df7.registerTempTable("df7")
        val num_count = sqlContext.sql("SELECT `Customer Number` , count(1) as cnt from df7 where ( `Critical Situation Description` REGEXP '[0-9]') group by `Customer Number` order by cnt desc limit 50")

        // New query without country code
        var cus_name = sqlContext.sql("SELECT DISTINCT ( `Customer Number` ), `Customer Name`  from df7 where ( `Customer Name` REGEXP '[A-Z]' )  ")

        val name = collection.mutable.Map[String, String]()
        val q_list7 = cus_name.collect
        i = 0
        for(i <- 0 to q_list7.length -1 ) {
          val c_code = q_list7(i)(0).toString
          val c_name = q_list7(i)(1).toString
          if (c_code != null && c_name != null) {
            name += c_code -> c_name
          }
        }

        val number = collection.mutable.Map[String, Int]()
        val q_listc = num_count.cache().collect

           def critsits = Action {
             implicit request =>

               var critRes = "["

               val j = 0

               for(j <- 0 to q_listc.length -1 ) {
                 if (name.contains(q_listc(j)(0).toString)) {

                   critRes += Json.toJson(FileCrit(j+1, name(q_listc(j)(0).toString), (q_listc(j)(1).toString).toInt))
                   critRes += ","
                   //jsn += "{\"CustomerName\":\""+name(q_listc(j)(0).toString)+"\""+","+"\"Number\":"+q_listc(j)(1)+"}"
                 }
               }
               critRes += "]"

               Ok(critRes)
           }

/*           def average = Action {
             implicit request =>

               val df = sqlContext.read.json("../Json-Data/pmr_data.json")
               df.registerTempTable("df")

               def datediff(s:String, e:String) : Int = {

                 val date_format = new SimpleDateFormat("yy/MM/dd hh:mm")
                 val st_date = date_format.parse(s)
                 val en_date = date_format.parse(e)
                 val diff = en_date.getTime() - st_date.getTime()
                 val diffdays = Math.ceil(((diff.toDouble)/(24 *60*60*1000).toDouble)).toInt
                 return diffdays
               }

               sqlContext.udf.register("datediff", datediff _)

         //      val av_count = sqlContext.sql("SELECT Severity,  avg(datediff((substr(`Created Date`,2,14)), (substr(`Last Altered Date`,2,14)))) " +
         //        "as days from df where `Last Altered Date` <> 'null' and `Created Date` <> 'null' and Severity <> 'null'  and Severity <> '' " +
         //        "group by Severity order by Severity")

               val av_count = sqlContext.sql("SELECT Severity, avg(datediff((substr(`Created Date`,2,14)), (substr(`Last Altered Date`,2,14)))) as days from df " +
                 "where `Last Altered Date` <> 'null' and `Created Date` <> 'null' and Severity <> 'null'  and Severity <> '' " +
                 "group by Severity order by Severity")

               val q_list = av_count.collect
               val pw = new PrintWriter(new File("/home/s/spark/avg_op"))
               val i = 0
               for(i <- 0 to q_list.length -1 ) {
                 pw.println("["+q_list(i)(0)+","+q_list(i)(1)+"]")
               }
               pw.close()

               Ok(views.html.index(" hai "+av_count))
           }

*/
            // active users query
  case class FileActi(Id: Int, CustomerName : String, Number : Int)
  implicit val activeusers = new Writes[FileActi] {
    def writes(location: FileActi) = Json.obj(
      "Id" -> location.Id,
      "Customer Name" -> location.CustomerName ,
      "Number" -> location.Number
    )
  }

  val df12 = sqlContext.read.json("../Json-Data/pmr_data.json")
  df12.registerTempTable("df12")

  val cus_num = sqlContext.sql("SELECT `Customer Number`, count(1) as cnt from df12 group by `Customer Number` order by cnt desc ")

  def myreplace(str: String) : String = {
    val updatedLine = str.replaceAll(",", " ");
    return (updatedLine)
  }

  val act_cus_name = sqlContext.sql("SELECT DISTINCT ( `Customer Number` ), (`Customer Name`)  from df12  limit 50 ")

  val act_name = collection.mutable.Map[String, String]()
  val q_lista = act_cus_name.collect
  i = 0
  for(i <- 0 to q_lista.length -1 ) {
    val c_code = q_lista(i)(0).toString
    val c_name = q_lista(i)(1).toString
    if (c_code != null && c_name != null) {
      act_name += c_code -> c_name
    }
  }

  val q_lista1 = cus_num.cache().collect

  def active = Action {
    implicit request =>
          var actiRes = "["
      val j = 0
       var k = 0
      for(j <- 0 to q_lista1.length -1 ) {
        if (q_lista1(j)(0) == null) {
          //pw.println("["+"NONE"+", "+q_list1(j)(1)+"]")
          actiRes += Json.toJson(FileActi(k+1, "NONE", (q_lista1(j)(1).toString).toInt)) +","
        }  else if (act_name.contains(q_lista1(j)(0).toString)) {
          //pw.println("["+act_name(q_list1(j)(0).toString)+","+q_list1(j)(1)+"]")
          actiRes += Json.toJson(FileActi(k+1, act_name(q_lista1(j)(0).toString), (q_lista1(j)(1).toString).toInt)) +","
        }
        k += 1
      }
      actiRes += "]"
      Ok(actiRes)
  }
  // Pie-Charts
  // redirecting to respective chart page
  def perPie = Action {
    implicit request =>
      Ok(views.html.charts.pie_charts.per_Pie())
  }

  def sentiPie = Action {
    implicit request =>
      Ok(views.html.charts.pie_charts.senti_Pie())
  }

  def geoPie = Action {
    implicit request =>
      Ok(views.html.charts.pie_charts.geo_Pie())
  }

  def codePie = Action {
    implicit request =>
      Ok(views.html.charts.pie_charts.code_Pie())
  }

  def critPie = Action {
    implicit request =>
      Ok(views.html.charts.pie_charts.crit_Pie())
  }

  def hardPie = Action {
    implicit request =>
      Ok(views.html.charts.pie_charts.hard_Pie())
  }

  def softPie = Action {
    implicit request =>
      Ok(views.html.charts.pie_charts.soft_Pie())
  }

  def monthPie = Action {
    implicit request =>
      Ok(views.html.charts.pie_charts.month_Pie())
  }

  def actiPie = Action {
    implicit request =>
      Ok(views.html.charts.pie_charts.acti_Pie())
  }

  // Column-Chart
  // redirecting to respective chart page

  def monthCol = Action {
    implicit request =>
      Ok(views.html.charts.col_charts.month_Col())
  }

  def hardCol = Action {
    implicit request =>
      Ok(views.html.charts.col_charts.hard_Col())
  }

  def softCol = Action {
    implicit request =>
      Ok(views.html.charts.col_charts.soft_Col())
  }

  def codeCol = Action {
    implicit request =>
      Ok(views.html.charts.col_charts.code_Col())
  }

  def sentiCol = Action {
    implicit request =>
      Ok(views.html.charts.col_charts.senti_Col())
  }

  def jsonChart = Action {
    implicit request =>

      Ok(views.html.temp())
  }
}