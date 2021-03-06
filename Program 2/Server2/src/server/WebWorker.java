 /**
* Web worker: an object of this class executes in its own new thread
* to receive and respond to a single HTTP request. After the constructor
* the object executes on its "run" method, and leaves when it is done.
*
* One WebWorker object is only responsible for one client connection. 
* This code uses Java threads to parallelize the handling of clients:
* each WebWorker runs in its own thread. This means that you can essentially
* just think about what is happening on one client at a time, ignoring 
* the fact that the entirety of the webserver execution might be handling
* other clients, too. 
*
* This WebWorker class (i.e., an object of this class) is where all the
* client interaction is done. The "run()" method is the beginning -- think
* of it as the "main()" for a client interaction. It does three things in
* a row, invoking three methods in this class: it reads the incoming HTTP
* request; it writes out an HTTP header to begin its response, and then it
* writes out some HTML content for the response content. HTTP requests and
* responses are just lines of text (in a very particular format). 
*
**/

import java.net.Socket;
import java.lang.Runnable;
import java.io.*;
import java.util.Date;
import java.text.DateFormat;
import java.util.TimeZone;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;

public class WebWorker implements Runnable
{

private Socket socket;
private String url;

/**
* Constructor: must have a valid open socket
**/
public WebWorker(Socket s)
{
   socket = s;
}

/**
* Worker thread starting point. Each worker handles just one HTTP 
* request and then returns, which destroys the thread. This method
* assumes that whoever created the worker created it with a valid
* open socket object.
**/
public void run()
{
   System.err.println("Handling connection...");
   try {
      InputStream  is = socket.getInputStream();
      OutputStream os = socket.getOutputStream();
      readHTTPRequest(is);
      writeHTTPHeader(os,"text/html");
      writeContent(os, url);
      os.flush();
      socket.close();
   } catch (Exception e) {
      System.err.println("Output error: "+e);
   }
   System.err.println("Done handling connection.");
   return;
}

/**
* Read the HTTP request header.
**/
private void readHTTPRequest(InputStream is)
{
   String line;
   BufferedReader r = new BufferedReader(new InputStreamReader(is));
   while (true) {
      try {
         while (!r.ready()) Thread.sleep(1);
         line = r.readLine();
         System.err.println("Request line: ("+line+")");
         if (line.length()==0) break;   
         if(line.substring(0, 3).equals("GET")){
             url = line.substring(5).split(" ")[0];
         }
      } catch (Exception e) {
         System.err.println("Request error: "+e);
         break;
      }
   }
   return;
}

/**
* Write the HTTP header lines to the client network connection.
* @param os is the OutputStream object to write to
* @param contentType is the string MIME content type (e.g. "text/html")
**/
private void writeHTTPHeader(OutputStream os, String contentType) throws Exception
{
    File page;
    //Reads images file and does not append .html
   if(url.contains(".png") || url.contains(".ico") || url.contains(".gif") || url.contains(".jpg") || url.contains(".jpeg")){
       page = new File(url.replace('\\', '/'));
   }
   else{
       //appends .html to read file.
       page = new File(url.replace('\\', '/')+".html");
   }
   Date d = new Date();
   DateFormat df = DateFormat.getDateTimeInstance();
   df.setTimeZone(TimeZone.getTimeZone("GMT"));
   if(page.isFile()){
       os.write("HTTP/1.1 200 OK\n".getBytes());
   }
   else{
      os.write("HTTP/1.1 404 OK\n".getBytes());
   }
   os.write("Date: ".getBytes());
   os.write((df.format(d)).getBytes());
   os.write("\n".getBytes());
   os.write("Server: Jon's very own server\n".getBytes());
   os.write("Connection: close\n".getBytes());
   os.write("Content-Type: ".getBytes());
   os.write(contentType.getBytes());
   os.write("\n\n".getBytes()); // HTTP header ends with 2 newlines
   return;
}

/**
* Write the data content to the client network connection. This MUST
* be done after the HTTP header has been written out.
* @param os is the OutputStream object to write to
**/
private void writeContent(OutputStream os, String url) throws Exception
{
    File page;
    //writes bytes from image file
   if(url.contains(".png") || url.contains(".ico") || url.contains(".gif") || url.contains(".jpg") || url.contains(".jpeg")){
       page = new File(url.replace('\\', '/'));
       byte[] fileContent = Files.readAllBytes(page.toPath());
       os.write(fileContent);
   }
   else{
       //writes bytes from html files
       page = new File(url.replace('\\', '/')+".html");
       if(page.isFile()){
             os.write(readFileToByteArray(page));
       }
       else{
         System.out.println(page);
         page = new File("pages/404.html");
         os.write(readFileToByteArray(page));
        }
   }
}
private static byte[] readFileToByteArray(File file){
        FileInputStream fis = null;
        // Creating a byte array using the length of the file
        // file.length returns long which is cast to int
        byte[] bArray = new byte[(int) file.length()];
        try{
            fis = new FileInputStream(file);
            fis.read(bArray);
            fis.close();        
        }catch(IOException ioExp){
            ioExp.printStackTrace();
        }   
        String s = new String(bArray, StandardCharsets.UTF_8);
        s = s.replace("<cs371date>", java.time.LocalDate.now()+"");
        s = s.replace("<cs371server>", "Joshua R. Alexander's Server");
        bArray = s.getBytes();
        return bArray;
    }

} // end class
