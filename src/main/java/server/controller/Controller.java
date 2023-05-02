package server.controller;


import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.ThreadContext;
import org.apache.logging.log4j.core.LoggerContext;
import org.apache.logging.log4j.core.config.Configuration;
import org.apache.logging.log4j.core.config.Configurator;
import org.apache.logging.log4j.core.config.LoggerConfig;
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
        ThreadContext.put("counter", String.valueOf(counter));
        requestLogger.info("Incoming request | #"+ counter +" | resource: /todo/health | HTTP Verb GET");
        requestLogger.debug("request #"+counter+" duration: "+new Date().getTime()+"ms");
        counter++;
        return "OK";
    }

    @PostMapping(value = "/todo",consumes = {MediaType.APPLICATION_JSON_VALUE})
    public ResponseEntity<Map<String,Object>> CreateNewTODO(@RequestBody Map<String,String> requestBody)
    {
        Map<String,Object> bodyResponse=new HashMap<>();
        String title =requestBody.get("title");
        Long dueDate =Long.valueOf(requestBody.get("dueDate"));

        ThreadContext.put("counter", String.valueOf(counter));
        requestLogger.info("Incoming request | #"+ counter +" | resource: /todo | HTTP Verb POST");
        requestLogger.debug("request #"+counter+" duration: "+new Date().getTime()+"ms");
        counter++;

        if(Utilities.checkIfExistByTitle(todoList,title))
        {
            String errorMessage="Error: TODO with the title " + title+ " already exists in the system";
            bodyResponse.put("errorMessage",errorMessage);
            todoLogger.error("Server encountered an error ! message: "+errorMessage);
            return new ResponseEntity<>(bodyResponse, HttpStatus.CONFLICT);
        }
        if(dueDate.compareTo(new Date().getTime())<0)
        {
            String errorMessage="Error: Can’t create new TODO that its due date is in the past";
            bodyResponse.put("errorMessage",errorMessage);
            todoLogger.error("Server encountered an error ! message: "+errorMessage);
            return new ResponseEntity<>(bodyResponse, HttpStatus.CONFLICT);
        }
        Todo todo =new Todo(title,requestBody.get("content"),dueDate);

        todoLogger.info("Creating new TODO with Title ["+title+"]");
        todoLogger.debug("Currently there are "+todoList.size()+" Todos in the system. " +
                "New TODO will be assigned with id "+todo.getId());

        todoList.add(todo);
        bodyResponse.put("result",todo.getId());
        return  new ResponseEntity<>(bodyResponse, HttpStatus.OK);
    }



    @GetMapping("/todo/size")
    public ResponseEntity<Map<String,Integer>> getTODOsCount(@RequestParam String status)
    {
        Map<String,Integer> bodyResponse=new HashMap<>();

        ThreadContext.put("counter", String.valueOf(counter));
        requestLogger.info("Incoming request | #"+ counter +" | resource: /todo/size | HTTP Verb GET");
        requestLogger.debug("request #"+counter+" duration: "+new Date().getTime()+"ms");
        counter++;

        if(status.equals("ALL"))
        {
            bodyResponse.put("result",todoList.size());
            todoLogger.info("Total TODOs count for state ALL is "+todoList.size());
            return new ResponseEntity<>(bodyResponse,HttpStatus.OK);
        }
        if(Utilities.STATUSES.contains(status))
        {
            ResponseEntity<Map<String,Integer>> responseEntity=Utilities.countTODO(todoList,status);
            int size=responseEntity.getBody().get("result");
            todoLogger.info("Total TODOs count for state "+status+" is "+size);
            return responseEntity;
        }

        todoLogger.error("");
        return new ResponseEntity<>(null,HttpStatus.BAD_REQUEST);
    }



    @GetMapping("/todo/content")
    public ResponseEntity<List<Todo>> getTODOsData(@RequestParam String status,@RequestParam(required = false) String sortBy)
    {
        List<Todo> bodyResponse=new ArrayList<>();

        ThreadContext.put("counter", String.valueOf(counter));
        requestLogger.info("Incoming request | #"+ counter +" | resource: /todo/content | HTTP Verb GET");
        requestLogger.debug("request #"+counter+" duration: "+new Date().getTime()+"ms");
        counter++;

        if( ( !Utilities.STATUSES.contains(status) && !status.equals("ALL") ) || ( sortBy!=null && !Utilities.SORTS_BY.contains(sortBy) ) )
        {
            todoLogger.error("");
            return new ResponseEntity<>(null,HttpStatus.BAD_REQUEST);
        }

        if(status.equals("ALL"))
            bodyResponse=todoList;
        else
            bodyResponse=Utilities.getTODO(todoList,status);

        if(sortBy==null||sortBy.equals("ID"))
            Utilities.SortById(bodyResponse);
        else
        {
            if (sortBy.equals("TITLE"))
                Utilities.SortByTitle(bodyResponse);
            if (sortBy.equals("DUE_DATE"))
                Utilities.SortByDate(bodyResponse);
        }

        if(sortBy==null)
            sortBy="ID";

        todoLogger.info("Extracting todos content. Filter: "+status+" | Sorting by: "+sortBy);
        todoLogger.debug("There are a total of "+todoList.size()+" todos in the system. " +
                "The result holds "+bodyResponse.size()+" todos");
        return new ResponseEntity<>(bodyResponse,HttpStatus.OK);
    }

    @PutMapping("/todo")
    public ResponseEntity<Map<String,String>> UpdateTODOStatus(@RequestParam Integer id,@RequestParam String status)
    {
        ThreadContext.put("counter", String.valueOf(counter));
        requestLogger.info("Incoming request | #"+ counter +" | resource: /todo | HTTP Verb PUT");
        requestLogger.debug("request #"+counter+" duration: "+new Date().getTime()+"ms");
        counter++;

        todoLogger.info("Update TODO id ["+id+"] state to "+status);

        if(!Utilities.STATUSES.contains(status)) {
            todoLogger.error("");
            return new ResponseEntity<>(null, HttpStatus.BAD_REQUEST);
        }

        Todo todo;
        String oldStatus;
        Map<String,String> bodyResponse=new HashMap<>();

        todo=Utilities.FindTODObyId(todoList,id);

        if(todo==null)
        {
            bodyResponse.put("errorMessage","Error: no such TODO with id "+id);
            todoLogger.error("Error: no such TODO with id "+id);
            return new ResponseEntity<>(bodyResponse,HttpStatus.NOT_FOUND);
        }

        oldStatus=todo.getStatus();
        todo.setStatus(status);
        bodyResponse.put("result",oldStatus);
        todoLogger.debug("Todo id ["+id+"] state change: "+oldStatus+" --> "+status);
        return new ResponseEntity<>(bodyResponse,HttpStatus.OK);
    }

    @DeleteMapping("/todo")
    public ResponseEntity<Map<String,Object>> DeleteTODO(@RequestParam Integer id)
    {
        Map<String,Object> bodyResponse=new HashMap<>();
        Todo todo =Utilities.FindTODObyId(todoList,id);

        ThreadContext.put("counter", String.valueOf(counter));
        requestLogger.info("Incoming request | #"+ counter +" | resource: /todo | HTTP Verb DELETE");
        requestLogger.debug("request #"+counter+" duration: "+new Date().getTime()+"ms");
        counter++;

        if(todo==null)
        {
            bodyResponse.put("errorMessage","Error: no such TODO with id "+id);
            todoLogger.error("Error: no such TODO with id "+id);
            return new ResponseEntity<>(bodyResponse,HttpStatus.NOT_FOUND);
        }
        todoLogger.info("Removing todo id "+id);
        todoList.remove(todo);
        todoLogger.debug("After removing todo id ["+id+"] there are "+todoList.size()+" TODOs in the system");
        bodyResponse.put("result",todoList.size());
        return new ResponseEntity<>(bodyResponse,HttpStatus.OK);
    }

    @GetMapping("/logs/level")
    public String getLoggerLevel(@RequestParam(name="logger-name") String loggerName)
    {
        String res="";

        ThreadContext.put("counter", String.valueOf(counter));
        requestLogger.info("Incoming request | #"+ counter +" | resource: /logs/level | HTTP Verb GET");
        requestLogger.debug("request #"+counter+" duration: "+new Date().getTime()+"ms");
        counter++;


        if(loggerName.equals("request-logger"))
            res=requestLogger.getLevel().toString().toUpperCase();

        else if(loggerName.equals("todo-logger"))
            res=todoLogger.getLevel().toString().toUpperCase();

        else res="Error:logger not found";

        return res;
    }


    @PutMapping("/logs/level")
    public String setLoggerLevel(@RequestParam(name="logger-name") String loggerName,@RequestParam(name="logger-level") String loggerLevel)
    {
        String res="";

        ThreadContext.put("counter", String.valueOf(counter));
        requestLogger.info("Incoming request | #"+ counter +" | resource: /logs/level | HTTP Verb PUT");
        requestLogger.debug("request #"+counter+" duration: "+new Date().getTime()+"ms");
        counter++;


        if(loggerLevel.equals("DEBUG")||loggerLevel.equals("INFO")||loggerLevel.equals("ERROR"))
        {
            if(loggerName.equals("request-logger"))
                res=requestLogger.getLevel().toString().toUpperCase();
            if(loggerName.equals("todo-logger"))
                res=todoLogger.getLevel().toString().toUpperCase();

            if(!res.equals(""))
                Configurator.setLevel(loggerName, Level.getLevel(loggerLevel));
            else
                res = "Error:logger not found";
        }
        else
        {
            res="Error:level not defined";
        }


        return res;
    }
}
