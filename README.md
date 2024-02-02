# BorderHopper
Open source game simiar to https://travle.earth/. Written as a university project. 

# Steps to setup the game

## Setting up the DB
Download and install PostgreSQL and pgAdmin 4 from the following link: https://www.postgresql.org/download/

Setup and remember the DB url, username and password

## Setting up the Spring project
- Download and open Spring Tool Suite 4 for Eclipse from https://spring.io/tools
- Open the root `BorderHopper` folder as the workplace
- File -> Open Projects from File System, select the `BorderHopperSpring` folder, and import the `BorderHopperSpring` project.
- Wait for the project to initialize
- Open the application.properties file to change the `spring.datasource.url`, `spring.datasource.username` and `spring.datasource.password`. File is located in src/main/resources inside the Package Exporer
- In the Package Exporer go to: src/main/java -> com.borderhopper, right click on `BorderHopperSpringApplication.java` and select Run As -> Spring Boot App
- The Project will start and will initialize the DB, wait for the initialization to complete

## Setting up the Vue.js frontend
- Inside the terminal navigate to the `BorderHopperVueJs` folder
- Run `npm install`
- Run `npm run serve`
- The game is ready to play at http://localhost:8080/
