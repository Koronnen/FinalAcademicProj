## Prerequisites & Version Support

Before deploying this web application, ensure that your environment provides database instances matching or exceeding the following specifications:

* **PostgreSQL**: Version **18.4-1** or newer.
Download driver here (Platform-Independent): https://jdbc.postgresql.org/download/ 
* **MySQL**: Version **8.0** or newer.
Download driver here (Platform-Independent): https://dev.mysql.com/downloads/connector/j/
* **Apache Derby**: Legacy network server compatibility supported on standard installations.
* **Java Environment**: JDK 17+ / Jakarta EE 10+ web container platform profile (e.g., Tomcat 10.1+).

---

## Setup & Deployment Configuration

To establish valid database pipelines, you **must modify the credentials** explicitly defined within the application deployment descriptor (`web.xml`) to align with your target runtime environments.

For the login credentials of the users (students/instructors), their default password is their **first name** in lowercase letters.

### Step 1: Open the Deployment Descriptor
Navigate to the root configuration map of your target build:
```bash
src/main/webapp/WEB-INF/web.xml
```
### Step 2: Change database credentials
In the ```web.xml```, change the database credentials for Derby, MySQL, and PostgreSQL to match yours.

### Step 3: Run Initiating SQL Scripts.
In the ```scripts/``` folder found in the working directory, there are 3 ```.txt``` files with the necessary SQL queries to initialize and populate the databases.

