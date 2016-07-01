# PMR-DataMining
Project Setup :
1. Install Play from below link
https://www.playframework.com/documentation/2.5.x/Installing
2. Check whether activator is running or not
-run below command in terminal :
activator
3. If activator is installed then you can directly go to project directory and run the project.
To start server, run the below command :
activator “run 8080”
Goto browser and type : http://localhost:8080/
Note :
1. The port no. 8080 is fixed for now, Project can't be run in any other ports.
2. Keep Project and Json-Data in same folder.
Project Flow:
Click on the query in UI(ex: Monthly Inflow(this is in main.scala.html))
It will invoke onclick method in Jquery(this method is in main.scala.html)
            a. Here it will load the chart from html page(i.e month_col.scala.html) and put that chart in the respective div in home page(i.e main.scala.html)
            b. Respective plain text will be shown in the div(i.e “monthly inflow” as heading)
            


Project Structure :
1. Controller : It contains implementation of all methods invoked from routes.
Ex : Homecontroller, Controller.
2.   Views : It contains all html pages.
Ex : month_col.scala.html, hard_pie.scala.html.
3.   Public : this folder contains all css files, js files and dojo js files.
Ex : claro.css, dojo.js
4.   Conf : this folder contains the routes file


