## Prerequisites & Version Support

Before deploying this web application, ensure that your environment provides database instances matching or exceeding the following specifications:

* **PostgreSQL**: Version **18.4-1** or newer.
* **MySQL**: Version **8.0** or newer.
* **Apache Derby**: Legacy network server compatibility supported on standard installations.
* **Java Environment**: JDK 17+ / Jakarta EE 10+ web container platform profile (e.g., Tomcat 10.1+).

---

## Setup & Deployment Configuration

To establish valid database pipelines, you **must modify the credentials** explicitly defined within the application deployment descriptor (`web.xml`) to align with your target runtime environments.

### Step 1: Open the Deployment Descriptor
Navigate to the root configuration map of your target build:
```bash
src/main/webapp/WEB-INF/web.xml
