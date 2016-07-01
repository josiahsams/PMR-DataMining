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

    //Spark Context Creation
   val sparkConf  = new SparkConf().setAppName("Geo location").setMaster("local")
   val sc         = new SparkContext(sparkConf)
   val sqlContext = new SQLContext(sc)

   var i = 0

      //  Monthly reports
    // Serialization of data frame to convert it into json output

     case class FileMonth(id: Int, Month: String, count: Int) //defining class to serialize data

     implicit val monthlyrep = new Writes[FileMonth] {
       def writes(location: FileMonth) = Json.obj(
         "Id" -> location.id,
         "text" -> location.Month,
         "y" -> location.count
       )
     }
      // software query data serialization
  case class FileSoft(Id: Int, Software : String, Count : Int)
  implicit val softwar = new Writes[FileSoft] {
    def writes(location: FileSoft) = Json.obj(
      "Id" -> location.Id,
      "text" -> location.Software ,
      "y" -> location.Count
    )
  }

  // serializing the hardware data
  case class FileHard(Id: Int, Hardware : String, Count : Int)
  implicit val hardwar = new Writes[FileHard] {
    def writes(location: FileHard) = Json.obj(
      "Id" -> location.Id,
      "text" -> location.Hardware ,
      "y" -> location.Count
    )
  }

  // Pervasive Issues query
  case class FilePerv(Id: Int, APAR : String, count : Int)

  // serializing the sql output and storing it into a json format
  implicit val perIssue = new Writes[FilePerv] {
    def writes(location: FilePerv) = Json.obj(
      "Id" -> location.Id,
      "APAR" -> location.APAR ,
      "Count" -> location.count
    )
  }

  // active users query
  // Serialization of active users data
  case class FileActi(Id: Int, CustomerName : String, Number : Int)
  implicit val activeusers = new Writes[FileActi] {
    def writes(location: FileActi) = Json.obj(
      "Id" -> location.Id,
      "CustomerName" -> location.CustomerName ,
      "Number" -> location.Number
    )
  }

  // Serialization of code fix ratio data
  case class FileCode(Id: Int, PMRType : String, Count : Int)
  implicit val codefix = new Writes[FileCode] {
    def writes(location: FileCode) = Json.obj(
      "Id" -> location.Id,
      "text" -> location.PMRType ,
      "y" -> location.Count
    )
  }

  // serializing sql output data to convert it into JSON
  case class FileGeo(Id: Int, CountryName : String, count : Int)
  implicit val geolocation = new Writes[FileGeo] {
    def writes(location: FileGeo) = Json.obj(
      "Id" -> location.Id,
      "CountryId" -> location.CountryName ,
      "Count" -> location.count
    )
  }

  // serializing sentiment analysis sql data
  case class FileSenti(id: Int, sentimentType: String, count: Int)
  implicit val sentim = new Writes[FileSenti] {
    def writes(location: FileSenti) = Json.obj(
      "Id" -> location.id,
      "text" -> location.sentimentType ,
      "y" -> location.count
    )
  }

  // serializing critical situation details
  case class FileCrit(Id: Int, CustomerName : String, Number : Int)
  implicit val critical = new Writes[FileCrit] {
    def writes(location: FileCrit) = Json.obj(
      "Id" -> location.Id,
      "CustomerName" -> location.CustomerName ,
      "Number" -> location.Number
    )
  }

  val dataFile3 = "../Json-Data/pmr_data.json"   // reading input data
     val df3 = sqlContext.read.json(dataFile3)      // converting input data into dataframe
     df3.registerTempTable("df3")                   // registering data frame into a table

      // sql query to get monthly info based on created date
     val m_count = sqlContext.sql("SELECT substr(`Created Date`, 5, 2), count(1) as cnt from df3 where `Created Date` <> 'null'  group by substr(`Created Date`, 5, 2)")
      // caching the query result
     val q_listm = m_count.cache().collect

    // method to give json data as output
     def monthly = Action {
          //syntax of play framework
       implicit request =>
         // creating a variable monthres to store serialized json output
         var monthRes = "["
         //  to map string variable("01") to month name("Jan")
         val months = collection.mutable.Map[String, String]("01" -> "Jan", "02" -> "Feb", "03" -> "Mar", "04" -> "Apr", "05" -> "May", "06" -> "Jun", "07" -> "Jul", "08" -> "Aug", "09" -> "Sep", "10" -> "Oct", "11" -> "Nov", "12" -> "Dec")
         // variable to store o/p data i.e countryName and pmr's generated
         val op = collection.mutable.Map[String, Int]()

         i = 0
         // converting numeric month("01") name into string month name("Jan")
         for(i <- 0 to q_listm.length -1 ) {
           // checking the respective month and mapping it accordingly
           if (months.contains((q_listm(i)(0)).toString)) {
             op += months((q_listm(i)(0)).toString) -> ((q_listm(i)(1).toString)).toInt                       // mapping into month
           }
         }
          //defining an array of months, to give ouput for only existing months
         val a_m = Array("Jan", "Feb", "Mar", "Apr", "May", "Jun", "Jul", "Aug", "Sep", "Oct", "Nov", "Dec")
         var j = 0
         for(i <- 0 to 11) {
           val mm = a_m(i)
           // check if the month is there in the sql result
           if (op.contains(mm)) {
             val cc = op(mm)
             // jsn += "{\"Month\":\""+mm+"\""+","+"\"Count\":"+cc+"}"
             monthRes += Json.toJson(FileMonth(j+1, mm.toString, (cc.toString).toInt))+","
           } else {
             val cc = 0
             // give zero count of pmr's for the months whose data is not available
            // monthRes += Json.toJson(FileMonth(i+1, mm.toString, (cc.toString).toInt))+","
           }
           j = j+1
         }
         monthRes += "]"
        // output raw json output to html
         Ok(monthRes)
     }


  // applying filter month for hardware data
  def hardware2(monthNo: String) = Action {
    implicit request =>
      import scala.io.Source
      // sql query to get the hardware info for givn month
      var h_count1 = sqlContext.sql(s"SELECT `CPU Type` , count(1) from df3 where `CPU Type` <> 'null' and substr(`Created Date`, 5, 2)= $monthNo group by `CPU Type`  order by `CPU Type`")
      // collecting the query result
      var q_listh1 = h_count1.collect()
      // variable to store json data
      var hardRes = "["
      // variable to store mapping informaiton
      val h_codes = collection.mutable.Map[String, String]()
      // collecting hardware codes from file
      for(line <- Source.fromFile("../Json-Data/hardware.codes").getLines()) {
        val code = line.split(" ", 2)           // splitting the hardware codes with space
        val code_name = code.toList
        h_codes += code_name(0) -> code_name(1)
      }
      // mapping count to zero for each hardware type
      val op = collection.mutable.Map[String, Int]("Power5" -> 0, "Power6" -> 0, "Power7" -> 0, "Power8" -> 0, "Unknown" -> 0)
      i = 0
      // based on sql result updating the count of each hardware respectively and storing it in op variable
      for(i <- 0 to q_listh1.length -1 ) {
        if (h_codes.contains((q_listh1(i)(0)).toString)) {
          val h_code_val = h_codes(q_listh1(i)(0).toString)
          val count = (q_listh1(i)(1).toString).toInt
          val c_count = (op(h_code_val.toString)).toInt
          op.update(h_code_val, (c_count+count))
        } else {
          val count = (q_listh1(i)(1).toString).toInt
          val c_count = op("Unknown")
          op.update("Unknown" , (count+c_count))              // updating the unknown count if no hardware type is found
        }
      }

      i = 0
      // converting the final result into a json format
      for ((key, value) <- op.toSeq.sortBy(-_._2)) {
        //jsn += "{\"HardWare\":\""+key+"\""+","+"\"Count\":"+value+"}"
        hardRes += Json.toJson(FileHard(i+1, key.toString, (value.toString).toInt)) // converting into key valu pairs
        hardRes += ","
        i += 1
      }
      hardRes += "]"
      // output the final json result
      Ok(hardRes)
  }

  // applying month filter to most pervasive issue query
          def pervasive2(monthNo: String) = Action {
            implicit request =>
              // sql query to get respective APAR count for PMR's
              val apars = sqlContext.sql(s"SELECT APAR , count(1) as cnt from df3 where ( APAR REGEXP '[0-9]' ) and substr(`Created Date`, 5, 2)= $monthNo group by APAR order by cnt desc limit 10")
              val q_listp1 = apars.cache().collect     // caching the sql result
              // variable to store the json result
              var pervasiveRes = "["
              i = 0
              // forming json string from sql output
              for(i <- 0 to q_listp1.length -1 ) {
                pervasiveRes += Json.toJson(FilePerv(i+1, q_listp1(i)(0).toString, (q_listp1(i)(1).toString).toInt))
                if(i<q_listp1.length-1)
                  pervasiveRes += ","
              }
              pervasiveRes += "]"
              // output the final json result
              Ok(pervasiveRes)
          }

        // software query after applying month filter
      def software2(monthNo: String) = Action {
        implicit request =>
          // query to get the given month software details
          var rel_adopt = sqlContext.sql(s"SELECT Product , count(1) from df3 where substr(`Created Date`, 5, 2)= $monthNo group by Product ")
          val q_listso1 = rel_adopt.collect //collecting the sql result
          var softRes = "["
          // mapping the software id to respective operating system version
          val rel = Map[String, String]("5765G0300" -> "AIX-5.3", "5765G6200" -> "AIX-6.1", "5765H4000" -> "AIX-7.1", "5765G3400" -> "VIOS")

          i = 0
          // storing the sql result into a json format and storing it in a string
          for(i <- 0 to q_listso1.length -1 ) {
            if (q_listso1(i)(0) != null && rel.contains(q_listso1(i)(0).toString)) {
              //jsn += "{\"Software Type\":\""+q_listso(i)(0)+"\""+","+"\"Count\":"+q_listso(i)(1)+"}"
              softRes += Json.toJson(FileSoft(i+1, rel(q_listso1(i)(0).toString), (q_listso1(i)(1).toString).toInt))+","
            } else if (q_listso1(i)(0) == null) {
              //jsn += "{\"Software Type\":\"Invalid\""+","+"\"Count\":"+q_listso(i)(1)+"}"
              softRes += Json.toJson(FileSoft(i+1, "Unknown", (q_listso1(i)(1).toString).toInt))+","
            }
          }
          softRes += "]"
          // output the final json result
          Ok(softRes)
      }

  //  Query for  Codefixratio
        def codeFixRatio2(monthNo: String) = Action {
           implicit request =>
             // query to select fixed PMR's
             val apar_count = sqlContext.sql(s"SELECT COUNT (DISTINCT APAR), count(*) from df3 where ( APAR REGEXP '[0-9]' ) and substr(`Created Date`, 5, 2)= $monthNo ")
             // query to select all PMR's
             val pmr_count = sqlContext.sql(s"SELECT COUNT(*)  from df3 where substr(`Created Date`, 5, 2)= $monthNo")
             // caching the query result
             val a_list1 = apar_count.collect()
             // caching the query result
             val p_list1 = pmr_count.collect()
             i=0
             var codeRes = "["
             codeRes += Json.toJson(FileCode(i+1, "Total PMRs", (p_list1(0)(0).toString).toInt))+","
             codeRes += Json.toJson(FileCode(i+2, "Fixed PMRs", (a_list1(0)(0).toString).toInt))+"]"
             // output the final json result
            Ok(codeRes)
            }

       def myreplace(str: String) : String = {
         val updatedLine = str.replaceAll(",", " ");
         return (updatedLine)
       }

// query to get active users list
       def active2(monthNo: String) = Action {
         implicit request =>
  // query to get pmr's raised by each customer and taking only top 10 customers
           val cus_num = sqlContext.sql(s"SELECT `Customer Number`, count(1) as cnt from df3 where substr(`Created Date`, 5, 2)= $monthNo group by `Customer Number` order by cnt desc ")
    //  collecting customer name and customer number
           val act_cus_name = sqlContext.sql(s"SELECT DISTINCT ( `Customer Number` ), (`Customer Name`)  from df3 where substr(`Created Date`, 5, 2)= $monthNo limit 10 ")
    //  variable to map two strings
           val act_name = collection.mutable.Map[String, String]()
    //  collecting the sql result
           val q_lista1 = act_cus_name.collect
           i = 0
    //  getting the active user name from customer number
           for(i <- 0 to q_lista1.length -1 ) {
             val c_code = q_lista1(i)(0).toString
             val c_name = q_lista1(i)(1).toString
             if (c_code != null && c_name != null) {
               act_name += c_code -> c_name
             }
           }
           // collecting customer number
           val q_lista2 = cus_num.collect

           var actiRes = "["
           val j = 0
            var k = 0

// converting the final result into json format
           for(j <- 0 to q_lista2.length -1 ) {
             if (q_lista2(j)(0) == null) {
               //pw.println("["+"NONE"+", "+q_list1(j)(1)+"]")
               actiRes += Json.toJson(FileActi(j+1, "Unknown", (q_lista2(j)(1).toString).toInt)) +","
             }  else if (act_name.contains(q_lista2(j)(0).toString)) {
               //pw.println("["+act_name(q_list1(j)(0).toString)+","+q_list1(j)(1)+"]")
               actiRes += Json.toJson(FileActi(j+1, act_name(q_lista2(j)(0).toString), (q_lista2(j)(1).toString).toInt)) +","
             }
             k += 1
           }
           actiRes += "]"
           // output the final json result
           Ok(actiRes)
       }
// mapping the operating system type back to software code
  val rel = Map[String, String]("AIX-5.3" -> "5765G0300", "AIX-6.1" -> "5765G6200", "AIX-7.1" -> "5765H4000", "VIOS" -> "5765G3400")

  // hardware query for two filters
//  def hardware3(monthNo: String, softType: String) = Action {
//    implicit request =>
//      import scala.io.Source
//      var temp = rel(softType).toString
//      System.out.println("Software Type "+rel(softType)) ;
//      var h_count1 = sqlContext.sql(s"SELECT `CPU Type` , count(1) from df3 where `CPU Type` <> 'null' and `Product`= $temp group by `CPU Type`  order by `CPU Type`")
//      var q_listh1 = h_count1.collect()
//
//      var hardRes = "["
//      val h_codes = collection.mutable.Map[String, String]()
//      for(line <- Source.fromFile("../Json-Data/hardware.codes").getLines()) {
//        val code = line.split(" ", 2)
//        val code_name = code.toList
//        h_codes += code_name(0) -> code_name(1)
//      }
//
//      val op = collection.mutable.Map[String, Int]("Power5" -> 0, "Power6" -> 0, "Power7" -> 0, "Power8" -> 0, "Unknown" -> 0)
//      i = 0
//      for(i <- 0 to q_listh1.length -1 ) {
//        if (h_codes.contains((q_listh1(i)(0)).toString)) {
//          val h_code_val = h_codes(q_listh1(i)(0).toString)
//          val count = (q_listh1(i)(1).toString).toInt
//          val c_count = (op(h_code_val.toString)).toInt
//          op.update(h_code_val, (c_count+count))
//        } else {
//          val count = (q_listh1(i)(1).toString).toInt
//          val c_count = op("Unknown")
//          op.update("Unknown" , (count+c_count))
//        }
//      }
//
//      i = 0
//      for ((key, value) <- op.toSeq.sortBy(-_._2)) {
//        //jsn += "{\"HardWare\":\""+key+"\""+","+"\"Count\":"+value+"}"
//        hardRes += Json.toJson(FileHard(i+1, key.toString, (value.toString).toInt))
//        hardRes += ","
//        i += 1
//      }
//      hardRes += "]"
//      Ok(hardRes)
//  }

  // Pervasive Issues for giving json output
  def pervasive3(monthNo: String) = Action {
    implicit request =>
      // getting APAR's count based on the month
      val apars = sqlContext.sql(s"SELECT APAR , count(1) as cnt from df3 where ( APAR REGEXP '[0-9]' ) and substr(`Created Date`, 5, 2)= $monthNo group by APAR order by cnt desc limit 10")
      // caching the query result
      val q_listp1 = apars.cache().collect

      var pervasiveRes = "["
      i = 0
      // forming json object to show raw json data in html page
      for(i <- 0 to q_listp1.length -1 ) {
        pervasiveRes += Json.toJson(FilePerv(i+1, q_listp1(i)(0).toString, (q_listp1(i)(1).toString).toInt))
        if(i<q_listp1.length-1)
          pervasiveRes += ","
      }
      pervasiveRes += "]"
      // output the final json result
      Ok(pervasiveRes)
  }

  //  Query for  Codefixratio to get json data
  def codeFixRatio3(monthNo: String) = Action {
    implicit request =>
      // getting fixed pmr's in the given month
      val apar_count = sqlContext.sql(s"SELECT COUNT (DISTINCT APAR), count(*) from df3 where ( APAR REGEXP '[0-9]' ) and substr(`Created Date`, 5, 2)= $monthNo ")
      // getting total pmr's in the month
      val pmr_count = sqlContext.sql(s"SELECT COUNT(*)  from df3 where substr(`Created Date`, 5, 2)= $monthNo")
      // collecting the query result
      val a_list1 = apar_count.collect()
      // collecting the query result
      val p_list1 = pmr_count.collect()
      i=0
      // converting the result into json format
      var codeRes = "["
      codeRes += Json.toJson(FileCode(i+1, "Total PMRs", (p_list1(0)(0).toString).toInt))+","
      codeRes += Json.toJson(FileCode(i+2, "Fixed PMRs", (a_list1(0)(0).toString).toInt))+"]"
      // output the final json result
      Ok(codeRes)
  }

  // active users query
  def active3(monthNo: String) = Action {
    implicit request =>
      // query to get customer number and PMR's raised by him
      val cus_num = sqlContext.sql(s"SELECT `Customer Number`, count(1) as cnt from df3 where substr(`Created Date`, 5, 2)= $monthNo group by `Customer Number` order by cnt desc ")
     // selecting both customer name and number
      val act_cus_name = sqlContext.sql(s"SELECT DISTINCT ( `Customer Number` ), (`Customer Name`)  from df3 where substr(`Created Date`, 5, 2)= $monthNo limit 10 ")
      // variable to map from customer number to customer name
      val act_name = collection.mutable.Map[String, String]()
      // collecting the query result
      val q_lista1 = act_cus_name.collect
      i = 0
      // getting customer name by mapping it to customer number
      for(i <- 0 to q_lista1.length -1 ) {
        val c_code = q_lista1(i)(0).toString
        val c_name = q_lista1(i)(1).toString
        if (c_code != null && c_name != null) {
          act_name += c_code -> c_name
        }
      }
      //
      val q_lista2 = cus_num.collect

      var actiRes = "["
      val j = 0
      var k = 0
// forming json data object
      for(j <- 0 to q_lista2.length -1 ) {
        if (q_lista2(j)(0) == null) {
          //pw.println("["+"NONE"+", "+q_list1(j)(1)+"]")
          actiRes += Json.toJson(FileActi(j+1, "Unknown", (q_lista2(j)(1).toString).toInt)) +","
        }  else if (act_name.contains(q_lista2(j)(0).toString)) {
          //pw.println("["+act_name(q_list1(j)(0).toString)+","+q_list1(j)(1)+"]")
          actiRes += Json.toJson(FileActi(j+1, act_name(q_lista2(j)(0).toString), (q_lista2(j)(1).toString).toInt)) +","
        }
        k += 1
      }
      actiRes += "]"
      // output the final json result to html
      Ok(actiRes)
  }

  // Query based on Geographic Location


   // Spark SQL query
   val c_count1 = sqlContext.sql("SELECT substr(PMR,11,3) , count(1) as cnt from df3 where PMR <> 'null' group by substr(PMR,11,3) order by cnt desc")
   // caching the data collected
   c_count1.cache().collect()

   def geoLoc = Action {
       implicit request =>
       // variable to store Result

         var geoRes = "["
         // country codes
         val c_codes = collection.mutable.Map[String, String]()
         // reading country codes
         var dataFile = "../Json-Data/country.codes"
        // splitting the country codes data based on first space in the line
         for(line <- Source.fromFile(dataFile).getLines()) {
           val code = line.split(" ", 2)
           val code_name = code.toList
           c_codes += code_name(0) -> code_name(1)
         }
          // mapping the country codes from string to integer
         val op = collection.mutable.Map[String, Int]()
         // collecting the result
         val q_listg = c_count1.collect()
         // taking the length of the file
         val countg = q_listg.length
         i = 0
           // converting the country codes into country names
         for(i <- 0 to q_listg.length -1 ) {
           if (c_codes.contains((q_listg(i)(0)).toString)) {
             op += c_codes((q_listg(i)(0)).toString) -> ((q_listg(i)(1).toString)).toInt
           }
         }
         i = 0
         // creating a json object string
         for ((key, value) <- op.toSeq.sortBy(-_._2)) {
           geoRes += Json.toJson(FileGeo(i+1, key.toString, (value.toString).toInt))

           if(i<(countg-14)) {
             geoRes += ","
           }
           i += 1
         }
         geoRes += "]"
         // output the final json result to html
         Ok(geoRes)
   }
        // Pervasive Issues

  // query to get APAR count Top-10
        val apars = sqlContext.sql("SELECT APAR , count(1) as cnt from df3 where ( APAR REGEXP '[0-9]' ) group by APAR order by cnt desc limit 10")
  // caching the query result
  val q_listp = apars.cache().collect
// method outputs raw json data
        def pervasive = Action {
          implicit request =>
            var pervasiveRes = "["
            i = 0
            // converting the query result to json string
            for(i <- 0 to q_listp.length -1 ) {
              pervasiveRes += Json.toJson(FilePerv(i+1, q_listp(i)(0).toString, (q_listp(i)(1).toString).toInt))
              if(i<q_listp.length-1)
                pervasiveRes += ","
            }
            pervasiveRes += "]"
            // output the final json result to html
            Ok(pervasiveRes)
        }

      //  Query for  Codefixratio

// selecting the distinct APAR's and counting the PMR's related to that particular APAR
     val apar_count = sqlContext.sql("SELECT COUNT (DISTINCT APAR), count(*) from df3 where ( APAR REGEXP '[0-9]' ) ")
  // selecting the total PMR's
     val pmr_count = sqlContext.sql("SELECT COUNT(*)  from df3")
  // caching the qury result
  val a_list = apar_count.cache().collect()
     val p_list = pmr_count.cache().collect()

  // gives the json output for codefixratio
      def codeFixRatio = Action {
         implicit request =>
           i = 0
           var codeRes = "["
  // Forming total PMR's Json string
           codeRes += Json.toJson(FileCode(i+1, "Total PMRs", (p_list(0)(0).toString).toInt))+","

           // Forming the fixed PMR's json string
           codeRes += Json.toJson(FileCode(i+2, "Fixed PMRs", (a_list(0)(0).toString).toInt))+"]"
          // output the final json result to html
           Ok(codeRes)
          }

        // HardWare related Query

       var h_count = sqlContext.sql("SELECT `CPU Type` , count(1) from df3 where `CPU Type` <> 'null' group by `CPU Type`  order by `CPU Type`")
        var q_listh = h_count.cache().collect()
// gives raw json output as a string in html
     def hardware = Action {
          implicit request =>
            import scala.io.Source
//  forming json string
            var hardRes = "["
//  mapping the hardware codes to respective hardware type(like Power8, power7, power6...etc)
            val h_codes = collection.mutable.Map[String, String]()

            for(line <- Source.fromFile("../Json-Data/hardware.codes").getLines()) {
              val code = line.split(" ", 2)
              val code_name = code.toList
              h_codes += code_name(0) -> code_name(1)
            }
// initializing the each hardware type count to zero
            val op = collection.mutable.Map[String, Int]("Power5" -> 0, "Power6" -> 0, "Power7" -> 0, "Power8" -> 0, "Unknown" -> 0)
            i = 0
// updating the hardware type count respectively based on PMR data
// if hardware type is not mentioned in PMR it will be categorized to unknown
            for(i <- 0 to q_listh.length -1 ) {
              if (h_codes.contains((q_listh(i)(0)).toString)) {
                val h_code_val = h_codes(q_listh(i)(0).toString)
                val count = (q_listh(i)(1).toString).toInt
                val c_count = (op(h_code_val.toString)).toInt
                op.update(h_code_val, (c_count+count))
              } else {
                val count = (q_listh(i)(1).toString).toInt
                val c_count = op("Unknown")
                op.update("Unknown" , (count+c_count))
              }
            }
// forming the json string
            i = 0
            for ((key, value) <- op.toSeq.sortBy(-_._2)) {
              //jsn += "{\"HardWare\":\""+key+"\""+","+"\"Count\":"+value+"}"
               hardRes += Json.toJson(FileHard(i+1, key.toString, (value.toString).toInt))
               hardRes += ","
              i += 1
            }
               hardRes += "]"
            // output the final json result to html
            Ok(hardRes)
        }

            // Sentiment Analysis

// it will output raw json output for sentiment analysis
// It is done based on the word count in the PMR text. Each word will be read and categorized into positive or negative or stop word. The respective word count will be increased and finally the pmr will be categorized based on the word count
            def sentiment = Action {
              implicit request =>
// reading json file
                val dataFile5 = "../Json-Data/sowmya.json"
                val df5 = sqlContext.read.json(dataFile5)
                df5.registerTempTable("df5")
                val pmr_count = sqlContext.sql("SELECT PMR_DATA from df5")

                val noval = 0;
                val pos_words = collection.mutable.Map[String, Int]()
// reading positive words
                var pos_wordsfile = "../Json-Data/pos-words.txt"
                for(line <- Source.fromFile(pos_wordsfile).getLines()) {
                  val words = line.split(" ", 2)
                  val  code_name = words.toList
                  pos_words += code_name(0) -> noval
                }
// reading negative words
                val neg_words = collection.mutable.Map[String, Int]()

                var neg_wordsfile = "../Json-Data/neg-words.txt"
                for(line <- Source.fromFile(neg_wordsfile).getLines()) {
                  val words = line.split(" ", 2)
                  val  code_name = words.toList
                  neg_words += code_name(0) -> noval
                }

// reading stop words
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
                var pos_pmr = 0 ;
                var neg_pmr = 0 ;
                var neu_pmr = 0 ;
// take the pmr read each word and categorize into pos. or neg. or stop word and increase the count accordingly
                for(j <- 0 to q_list.length - 1) {
                  sentiment = 0
                  flag = 0
                  val pmr_words = (q_list(j).toString).split(" ")
                  val words_list = pmr_words.toList
                  for(i <- 0 to words_list.length -1 ) {
                    if (stop_words.contains((words_list(i)).toString)) {        // checking for stop words
                      flag = 1
                    }
                    else if (pos_words.contains((words_list(i)).toString)) {     // checking for positive words
                      if (flag == 1) {
                        sentiment -= 1
                        flag = 0
                      } else {
                        sentiment += 1
                      }
                    }
                    else if (neg_words.contains((words_list(i)).toString)) {      // checking for negative words
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
                //forming the json object of the sentiment data
                for ((key, value) <- op.toSeq.sortBy(-_._2)) {
                  // jsn += "{\"Sentiment Type\":\""+key+"\""+","+"\"Count\":"+value+"}"
                  sentiRes += Json.toJson(FileSenti(i+1, (key.toString), (value.toString).toInt))
                  if(i<2)
                    sentiRes += ","
                  i = i+1
                }
                sentiRes += "]"
// output the json object to html page
                Ok(sentiRes)
            }

              // Software Reports query
// query to group pmr's based on the product type
     var rel_adopt = sqlContext.sql("SELECT Product , count(1) from df3 group by Product ")
     // caching the query result
     val q_listso = rel_adopt.cache().collect
// method to give raw json data for software query
              def software = Action {
                implicit request =>
                  var softRes = "["
                  val rel = Map[String, String]("5765G0300" -> "AIX-5.3", "5765G6200" -> "AIX-6.1", "5765H4000" -> "AIX-7.1", "5765G3400" -> "VIOS")
                  i = 0
                  // forming the json object
                  for(i <- 0 to q_listso.length -1 ) {
                    if (q_listso(i)(0) != null && rel.contains(q_listso(i)(0).toString)) {
                      //jsn += "{\"Software Type\":\""+q_listso(i)(0)+"\""+","+"\"Count\":"+q_listso(i)(1)+"}"
                      softRes += Json.toJson(FileSoft(i+1, rel(q_listso(i)(0).toString), (q_listso(i)(1).toString).toInt))+","

                    } else if (q_listso(i)(0) == null) {
                      //jsn += "{\"Software Type\":\"Invalid\""+","+"\"Count\":"+q_listso(i)(1)+"}"
                      softRes += Json.toJson(FileSoft(i+1, "Unknown", (q_listso(i)(1).toString).toInt))+","
                    }
                  }
                  softRes += "]"
                  // output the json string into html
                  Ok(softRes)
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
// query to get pmr count based on the customer number
     val cus_num = sqlContext.sql("SELECT `Customer Number`, count(1) as cnt from df3 group by `Customer Number` order by cnt desc ")
     // getting the customer number and customer name
     val act_cus_name = sqlContext.sql("SELECT DISTINCT ( `Customer Number` ), (`Customer Name`)  from df3  limit 10 ")

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
// caching the query result
     val q_lista1 = cus_num.cache().collect
// method outputs the raw json output for active users list
     def active = Action {
       implicit request =>
             var actiRes = "["
         val j = 0
          var k = 0
// forming json object
         for(j <- 0 to q_lista1.length -1 ) {
           if (q_lista1(j)(0) == null) {
             //pw.println("["+"NONE"+", "+q_list1(j)(1)+"]")
             actiRes += Json.toJson(FileActi(j+1, "Unknown", (q_lista1(j)(1).toString).toInt)) +","
           }  else if (act_name.contains(q_lista1(j)(0).toString)) {
             //pw.println("["+act_name(q_list1(j)(0).toString)+","+q_list1(j)(1)+"]")
             actiRes += Json.toJson(FileActi(j+1, act_name(q_lista1(j)(0).toString), (q_lista1(j)(1).toString).toInt)) +","
           }
           k += 1
         }
         actiRes += "]"
         // output the raw json to html
         Ok(actiRes)
     }

   // critsits query
// query to get customer numbers who faced critical situation details
   val num_count = sqlContext.sql("SELECT `Customer Number` , count(1) as cnt from df3 where ( `Critical Situation Description` REGEXP '[0-9]') group by `Customer Number` order by cnt desc limit 10")

   // New query without country code
   var cus_name = sqlContext.sql("SELECT DISTINCT ( `Customer Number` ), `Customer Name`  from df3 where ( `Customer Name` REGEXP '[A-Z]' )  ")
// collecting the result
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
// method to give raw json ouput for critical situation
   def critsits = Action {
     implicit request =>
       var critRes = "["

       val j = 0
// forming the json string
       for(j <- 0 to q_listc.length -1 ) {
         if (name.contains(q_listc(j)(0).toString)) {

           critRes += Json.toJson(FileCrit(j+1, name(q_listc(j)(0).toString), (q_listc(j)(1).toString).toInt))
           critRes += ","
           //jsn += "{\"CustomerName\":\""+name(q_listc(j)(0).toString)+"\""+","+"\"Number\":"+q_listc(j)(1)+"}"
         }
       }
       critRes += "]"
// output the json result to html
       Ok(critRes)
   }

   //Pie-Charts
   //redirecting to respective chart page
// the following methods will redirect the url in routes to respective html page
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

//      request.session.get("HomePage").map{ user => Ok(views.html.index("Hai"))
//      }.getOrElse{
//        Ok(views.html.charts.col_charts.month_Col())
//      } ;
//     // System.out.println("tmp "+tmp) ;
//
//      val stm = request.session + ("colchart"->"Chart") ;
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

  def worldData = Action {
    implicit request =>
      val worlddata = scala.io.Source.fromFile("../Json-Data/world_countries.json").getLines().mkString
      Ok(worlddata)
  }

  def critCol = Action{
    implicit request =>
    Ok(views.html.charts.col_charts.crit_Col())
  }

  def actiCol = Action{
    implicit request =>
      Ok(views.html.charts.col_charts.acti_Col())
  }

  def actiTable = Action {
    implicit request =>
      Ok(views.html.charts.tables.acti_Table())
  }

  def critTable = Action {
    implicit request=>
    Ok(views.html.charts.tables.crit_Table())
  }

  def perTable = Action {
    implicit request =>
      Ok(views.html.charts.tables.per_Table())
  }

  // Redirecting to Second page

  def secondPage = Action {
    implicit request =>
          Ok(views.html.charts.second())
  }

  def hardPie2 = Action {
    implicit request =>
      Ok(views.html.charts.pie_charts.hard_Pie2())
  }

  def perTable2 = Action {
    implicit request =>
    Ok(views.html.charts.tables.per_Table2())
  }

  def softPie2 = Action {
    implicit request =>
      Ok(views.html.charts.pie_charts.soft_Pie2())
  }

  def codePie2 = Action {
    implicit request =>

      Ok(views.html.charts.pie_charts.code_Pie2())
  }

  def actiTable2 = Action {
    implicit request =>
      Ok(views.html.charts.tables.acti_Table2())
  }

  // Redirecting to Third Page i.e software page
  def softwarePage = Action {
    implicit request =>
      Ok(views.html.charts.software())
  }

  def hardPie3 = Action {
    implicit request =>
      Ok(views.html.charts.pie_charts.hard_Pie3())
  }
}