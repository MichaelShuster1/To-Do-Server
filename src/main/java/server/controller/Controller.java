package server.controller;


import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.ThreadContext;
import org.apache.logging.log4j.core.config.Configurator;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import server.model.Todo;
import server.model.Utilities;

import java.util.*;

@RestController
public class Controller
{
    private Logger requestLogger = LogManager.getLogger("request-logger");

    private Logger todoLogger= LogManager.getLogger("todo-logger");


    private int counter=1;
    private List<Todo> todoList=new ArrayList<>();

    @GetMapping("/todo/health")
    public String Health()
    {
        long start=System.currentTimeMillis();
        ThreadContext.put("counter", String.valueOf(counter));
        requestLogger.info("Incoming request | #"+ counter +" | resource: /todo/health | HTTP Verb GET");
        requestLogger.debug("request #"+counter+" duration: "+(System.currentTimeMillis()-start)+"ms");
        counter++;
        return "OK";
    }

    @PostMapping(value = "/todo",consumes = {MediaType.APPLICATION_JSON_VALUE})
    public ResponseEntity<Map<String,Object>> CreateNewTODO(@RequestBody Map<String,String> requestBody)
    {
        long start=System.currentTimeMillis();
        Map<String,Object> bodyResponse=new HashMap<>();
        String title =requestBody.get("title");
        Long dueDate =Long.valueOf(requestBody.get("dueDate"));

        ThreadContext.put("counter", String.valueOf(counter));
        requestLogger.info("Incoming request | #"+ counter +" | resource: /todo | HTTP Verb POST");
        counter++;

        todoLogger.info("Creating new TODO with Title ["+title+"]");

        if(Utilities.checkIfExistByTitle(todoList,title))
        {
            String errorMessage="Error: TODO with the title " + title+ " already exists in the system";
            bodyResponse.put("errorMessage",errorMessage);
            todoLogger.error(errorMessage);
            requestLogger.debug("request #"+counter+" duration: "+(System.currentTimeMillis()-start)+"ms");
            return new ResponseEntity<>(bodyResponse, HttpStatus.CONFLICT);
        }
        if(dueDate.compareTo(new Date().getTime())<0)
        {
            String errorMessage="Error: Can’t create new TODO that its due date is in the past";
            bodyResponse.put("errorMessage",errorMessage);
            todoLogger.error(errorMessage);
            requestLogger.debug("request #"+counter+" duration: "+(System.currentTimeMillis()-start)+"ms");
            return new ResponseEntity<>(bodyResponse, HttpStatus.CONFLICT);
        }

        Todo todo =new Todo(title,requestBody.get("content"),dueDate);

        todoLogger.debug("Currently there are "+todoList.size()+" TODOs in the system. " +
                "New TODO will be assigned with id "+todo.getId());

        todoList.add(todo);
        bodyResponse.put("result",todo.getId());
        requestLogger.debug("request #"+counter+" duration: "+(System.currentTimeMillis()-start)+"ms");
        return  new ResponseEntity<>(bodyResponse, HttpStatus.OK);
    }



    @GetMapping("/todo/size")
    public ResponseEntity<Map<String,Integer>> getTODOsCount(@RequestParam String status)
    {
        if( !status.equals("ALL") && !Utilities.STATUSES.contains(status))
        {
            return new ResponseEntity<>(null,HttpStatus.BAD_REQUEST);
        }

        long start=System.currentTimeMillis();
        Map<String,Integer> bodyResponse=new HashMap<>();

        ThreadContext.put("counter", String.valueOf(counter));
        requestLogger.info("Incoming request | #"+ counter +" | resource: /todo/size | HTTP Verb GET");
        counter++;

        if(status.equals("ALL"))
        {
            bodyResponse.put("result",todoList.size());
            todoLogger.info("Total TODOs count for state ALL is "+todoList.size());
            requestLogger.debug("request #"+counter+" duration: "+(System.currentTimeMillis()-start)+"ms");
            return new ResponseEntity<>(bodyResponse,HttpStatus.OK);
        }
        if(Utilities.STATUSES.contains(status))
        {
            ResponseEntity<Map<String,Integer>> responseEntity=Utilities.countTODO(todoList,status);
            int size=responseEntity.getBody().get("result");
            todoLogger.info("Total TODOs count for state "+status+" is "+size);
            requestLogger.debug("request #"+counter+" duration: "+(System.currentTimeMillis()-start)+"ms");
            return responseEntity;
        }
        return null;
    }



    @GetMapping("/todo/content")
    public ResponseEntity<Map<String,List<Todo>>> getTODOsData(@RequestParam String status,@RequestParam(required = false) String sortBy)
    {
        if( ( !Utilities.STATUSES.contains(status) && !status.equals("ALL") ) || ( sortBy!=null && !Utilities.SORTS_BY.contains(sortBy) ) )
        {
            return new ResponseEntity<>(null,HttpStatus.BAD_REQUEST);
        }

        long start=System.currentTimeMillis();
        List<Todo> res =new ArrayList<>();

        ThreadContext.put("counter", String.valueOf(counter));
        requestLogger.info("Incoming request | #"+ counter +" | resource: /todo/content | HTTP Verb GET");
        counter++;


        if(status.equals("ALL"))
            res =todoList;
        else
            res =Utilities.getTODO(todoList,status);

        if(sortBy==null||sortBy.equals("ID"))
            Utilities.SortById(res);
        else
        {
            if (sortBy.equals("TITLE"))
                Utilities.SortByTitle(res);
            if (sortBy.equals("DUE_DATE"))
                Utilities.SortByDate(res);
        }

        if(sortBy==null)
            sortBy="ID";

        todoLogger.info("Extracting todos content. Filter: "+status+" | Sorting by: "+sortBy);
        todoLogger.debug("There are a total of "+todoList.size()+" todos in the system. " +
                "The result holds "+ res.size()+" todos");
        requestLogger.debug("request #"+counter+" duration: "+(System.currentTimeMillis()-start)+"ms");
        Map<String,List<Todo>> bodyResponse=new HashMap<>();
        bodyResponse.put("result",res);
        return new ResponseEntity<>(bodyResponse,HttpStatus.OK);
    }

    @PutMapping("/todo")
    public ResponseEntity<Map<String,String>> UpdateTODOStatus(@RequestParam Integer id,@RequestParam String status)
    {
        if(!Utilities.STATUSES.contains(status)) {
            return new ResponseEntity<>(null, HttpStatus.BAD_REQUEST);
        }

        long start=System.currentTimeMillis();
        ThreadContext.put("counter", String.valueOf(counter));
        requestLogger.info("Incoming request | #"+ counter +" | resource: /todo | HTTP Verb PUT");
        counter++;

        todoLogger.info("Update TODO id ["+id+"] state to "+status);

        String oldStatus;
        Map<String,String> bodyResponse=new HashMap<>();

        Todo todo=Utilities.FindTODObyId(todoList,id);

        if(todo==null)
        {
            bodyResponse.put("errorMessage","Error: no such TODO with id "+id);
            todoLogger.error("Error: no such TODO with id "+id);
            requestLogger.debug("request #"+counter+" duration: "+(System.currentTimeMillis()-start)+"ms");
            return new ResponseEntity<>(bodyResponse,HttpStatus.NOT_FOUND);
        }

        oldStatus=todo.getStatus();
        todo.setStatus(status);
        bodyResponse.put("result",oldStatus);
        todoLogger.debug("Todo id ["+id+"] state change: "+oldStatus+" --> "+status);
        requestLogger.debug("request #"+counter+" duration: "+(System.currentTimeMillis()-start)+"ms");
        return new ResponseEntity<>(bodyResponse,HttpStatus.OK);
    }

    @DeleteMapping("/todo")
    public ResponseEntity<Map<String,Object>> DeleteTODO(@RequestParam Integer id)
    {
        long start=System.currentTimeMillis();
        Map<String,Object> bodyResponse=new HashMap<>();
        Todo todo =Utilities.FindTODObyId(todoList,id);

        ThreadContext.put("counter", String.valueOf(counter));
        requestLogger.info("Incoming request | #"+ counter +" | resource: /todo | HTTP Verb DELETE");
        counter++;

        todoLogger.info("Removing todo id "+id);

        if(todo==null)
        {
            bodyResponse.put("errorMessage","Error: no such TODO with id "+id);
            todoLogger.error("Error: no such TODO with id "+id);
            requestLogger.debug("request #"+counter+" duration: "+(System.currentTimeMillis()-start)+"ms");
            return new ResponseEntity<>(bodyResponse,HttpStatus.NOT_FOUND);
        }
        todoList.remove(todo);
        todoLogger.debug("After removing todo id ["+id+"] there are "+todoList.size()+" TODOs in the system");
        bodyResponse.put("result",todoList.size());
        requestLogger.debug("request #"+counter+" duration: "+(System.currentTimeMillis()-start)+"ms");
        return new ResponseEntity<>(bodyResponse,HttpStatus.OK);
    }

    @GetMapping("/logs/level")
    public String getLoggerLevel(@RequestParam(name="logger-name") String loggerName)
    {
        long start=System.currentTimeMillis();
        String res="";

        ThreadContext.put("counter", String.valueOf(counter));
        requestLogger.info("Incoming request | #"+ counter +" | resource: /logs/level | HTTP Verb GET");;
        counter++;


        if(loggerName.equals("request-logger"))
            res=requestLogger.getLevel().toString().toUpperCase();

        else if(loggerName.equals("todo-logger"))
            res=todoLogger.getLevel().toString().toUpperCase();

        else res="Error:logger not found";

        requestLogger.debug("request #"+counter+" duration: "+(System.currentTimeMillis()-start)+"ms");
        return res;
    }


    @PutMapping("/logs/level")
    public String setLoggerLevel(@RequestParam(name="logger-name") String loggerName,@RequestParam(name="logger-level") String loggerLevel)
    {
        long start=System.currentTimeMillis();
        String res="";

        ThreadContext.put("counter", String.valueOf(counter));
        requestLogger.info("Incoming request | #"+ counter +" | resource: /logs/level | HTTP Verb PUT");
        counter++;


        if(loggerLevel.equals("DEBUG")||loggerLevel.equals("INFO")||loggerLevel.equals("ERROR"))
        {
            if(loggerName.equals("request-logger")||loggerName.equals("todo-logger"))
            {
                Configurator.setLevel(loggerName, Level.getLevel(loggerLevel));

                if(loggerName.equals("request-logger"))
                    res=requestLogger.getLevel().toString().toUpperCase();

                if(loggerName.equals("todo-logger"))
                    res=todoLogger.getLevel().toString().toUpperCase();
            }
            else
                res = "Error:logger not found";
        }
        else
        {
            res="Error:level not defined";
        }

        requestLogger.debug("request #"+counter+" duration: "+(System.currentTimeMillis()-start)+"ms");
        return res;
    }
}
