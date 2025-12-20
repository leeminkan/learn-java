# ☕ Java Servlet Hello World Application

This repository contains a simple "Hello World" Java Servlet application, built using **Maven** and deployed on **Apache Tomcat**. This project serves as a starting point for learning core Java web development concepts.

-----

## 🛠️ Prerequisites

To build and run this application, you will need the following installed on your machine:

* **Java Development Kit (JDK):** Version 17 or higher (Recommended).
* **Apache Maven:** Used for project management and dependency handling.
* **IntelliJ IDEA (Ultimate Edition Recommended):** The IDE used for development and running the server.
* **Apache Tomcat:** A web container (server) to deploy and run the servlet. (Version 9 or 10 recommended).

-----

## 🚀 Setup and Installation

### 1\. Clone the Repository

```bash
git clone [YOUR_REPOSITORY_URL]
cd [YOUR_PROJECT_FOLDER]
```

### 2\. Configure Dependencies (Maven)

Ensure your `pom.xml` file includes the necessary Jakarta Servlet API dependency. This ensures the project compiles correctly against the standard servlet specification.

> **Excerpt from `pom.xml`:**
>
> ```xml
> <dependency>
>     <groupId>jakarta.servlet</groupId>
>     <artifactId>jakarta.servlet-api</artifactId>
>     <version>6.0.0</version> >     <scope>provided</scope>
> </dependency>
> ```

### 3\. Grant Tomcat Execution Permissions (macOS/Linux Fix)

If you are on macOS or Linux, the Tomcat startup scripts might lack execution permissions. If you see a `Permission denied` error, run these commands in your terminal, replacing the path with your actual Tomcat directory:

```bash
# Navigate to the bin directory of your Tomcat installation
cd /path/to/apache-tomcat-X.X.X/bin

# Grant execute permission to all shell scripts
chmod +x *.sh
```

-----

## 🏃 Running the Application in IntelliJ IDEA

### 1\. Configure the Tomcat Server

1.  In IntelliJ IDEA, go to **Run** -\> **Edit Configurations...**.
2.  Click the **`+`** sign and select **"Tomcat Server"** -\> **"Local"**.
3.  Under the **Server** tab, click **Configure...** and point to the root directory of your **Apache Tomcat installation**.

### 2\. Configure Deployment

1.  In the same **Run/Debug Configurations** window, go to the **Deployment** tab.
2.  Click the **`+`** button and select **"Artifact..."**.
3.  Select the project's deployable artifact (it should be named something like `your-project-name:war exploded`).
4.  Set the **Application context** to a simple path, e.g., `/myapp`.

### 3\. Start the Server

1.  Click **OK** to save the configuration.
2.  Click the **Run** button (green triangle) in IntelliJ IDEA.

-----

## 🌐 Accessing the Servlet

Once the server starts successfully, open your web browser and navigate to the following URL:

```
http://localhost:8080/myapp/hello
```

* **`8080`**: Tomcat's default port (change if necessary).
* **`/myapp`**: The **Application Context** you defined in the Deployment settings.
* **`/hello`**: The **URL pattern** defined by the `@WebServlet("/hello")` annotation in the `HelloWorldServlet.java` file.

You should see the dynamic "Hello from a Java Servlet\!" output.

-----

## 📝 Code Structure

The core logic resides in a single servlet file:

* `src/main/java/com/example/app/HelloWorldServlet.java`

This class:

1.  **Extends** `HttpServlet`.
2.  Uses the **`@WebServlet("/hello")`** annotation for URL mapping.
3.  Overrides the **`doGet(HttpServletRequest, HttpServletResponse)`** method to handle HTTP GET requests.
4.  Uses `response.getWriter().println()` to generate the HTML response dynamically.

